package com.simpleweb.affaldsguidedanmark;

import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.File;

public class PhotoSearchFragment extends Fragment {
    private static final String TAG = "PhotoSearch";
    private final ExecutorService imageExecutor = Executors.newSingleThreadExecutor();
    private PhotoSelectionView photoSelection;
    private LinearLayout candidatesContainer;
    private TextView hint;
    private View loadingIndicator;
    private TextView resultsHeading;
    private TextView resultsNote;
    private Button analyzeSelection, analyzeFullImage;
    private Button replacePhotoButton;
    private Bitmap bitmap;
    private Uri pendingCameraUri;
    private final ActivityResultLauncher<PickVisualMediaRequest> pickReplacementPhoto =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) loadReplacementPhoto(uri);
            });
    private final ActivityResultLauncher<Uri> takeReplacementPhoto =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (Boolean.TRUE.equals(success) && pendingCameraUri != null)
                    loadReplacementPhoto(pendingCameraUri);
            });
    private boolean recordedSelection;
    private TrashDB trashDB;
    private final ImageLabeler labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS);
    private final TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent,
                                                   @Nullable Bundle state) {
        View root = inflater.inflate(R.layout.fragment_photo_search, parent, false);
        trashDB = new TrashDB(requireContext().getResources());
        photoSelection = root.findViewById(R.id.photoSelection);
        candidatesContainer = root.findViewById(R.id.photoCandidates);
        hint = root.findViewById(R.id.photoHint);
        loadingIndicator = root.findViewById(R.id.photoLoading);
        String loadingText = InfoPageText.get(requireContext(), "photosearchview.001");
        ((TextView) root.findViewById(R.id.photoLoadingText)).setText(loadingText);
        root.findViewById(R.id.photoLoadingSpinner).setContentDescription(loadingText);
        resultsHeading = root.findViewById(R.id.photoResultsHeading);
        resultsNote = root.findViewById(R.id.photoResultsNote);
        analyzeSelection = root.findViewById(R.id.analyzeSelection);
        analyzeFullImage = root.findViewById(R.id.analyzeFullImage);
        replacePhotoButton = root.findViewById(R.id.replacePhotoButton);
        photoSelection.setOnSelectionChangedListener(hasSelection ->
                analyzeSelection.setVisibility(hasSelection ? View.VISIBLE : View.GONE));
        ((TextView) root.findViewById(R.id.photoTitle)).setText(InfoPageText.get(requireContext(), "photosearchview.005"));
        hint.setText(InfoPageText.get(requireContext(), "photo.objectSelectionHint"));
        resultsHeading.setText(InfoPageText.get(requireContext(), "photosearchview.003"));
        resultsNote.setText(InfoPageText.get(requireContext(), "photosearchview.004"));
        analyzeSelection.setText(InfoPageText.get(requireContext(), "photo.analyzeSelection"));
        analyzeFullImage.setText(InfoPageText.get(requireContext(), "photo.useFullImage"));
        boolean fromCamera = getArguments() != null
                && "camera".equals(getArguments().getString("photoSource"));
        replacePhotoButton.setText(InfoPageText.get(requireContext(),
                fromCamera ? "photo.takeAnother" : "photo.chooseAnother"));
        replacePhotoButton.setOnClickListener(view -> {
            if (fromCamera) openCameraForReplacement();
            else pickReplacementPhoto.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });
        analyzeSelection.setOnClickListener(view -> {
            Bitmap cropped = photoSelection.selectedBitmap();
            if (cropped == null) {
                hint.setText(InfoPageText.get(requireContext(), "photo.objectSelectionHint"));
            } else {
                analyze(cropped);
            }
        });
        analyzeFullImage.setOnClickListener(view -> {
            if (bitmap != null) {
                photoSelection.clearSelection();
                analyze(bitmap);
            }
        });
        setBusy(true);
        String uriValue = getArguments() == null ? null : getArguments().getString("photoUri");
        if (uriValue == null) {
            showFailure();
        } else {
            imageExecutor.execute(() -> decodeImage(Uri.parse(uriValue)));
        }
        return root;
    }

    private void decodeImage(Uri uri) {
        try {
            // ImageDecoder applies image orientation and samples before allocating a full-size bitmap.
            ImageDecoder.Source source = ImageDecoder.createSource(requireContext().getContentResolver(), uri);
            Bitmap decoded = ImageDecoder.decodeBitmap(source, (decoder, info, input) -> {
                int width = info.getSize().getWidth(), height = info.getSize().getHeight();
                int sample = 1;
                while (Math.max(width / sample, height / sample) > 2048) sample *= 2;
                decoder.setTargetSampleSize(sample);
                decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
            });
            if (isAdded()) requireActivity().runOnUiThread(() -> {
                bitmap = decoded;
                photoSelection.setBitmap(decoded);
                candidatesContainer.removeAllViews();
                resultsHeading.setVisibility(View.GONE);
                resultsNote.setVisibility(View.GONE);
                hint.setText(InfoPageText.get(requireContext(), "photo.objectSelectionHint"));
                hint.setVisibility(View.VISIBLE);
                setBusy(false);
            });
        } catch (Exception error) {
            Log.e(TAG, "Could not decode selected image", error);
            if (isAdded()) requireActivity().runOnUiThread(this::showFailure);
        }
    }

    private void analyze(Bitmap image) {
        setBusy(true);
        candidatesContainer.removeAllViews();
        resultsHeading.setVisibility(View.GONE);
        resultsNote.setVisibility(View.GONE);
        hint.setVisibility(View.GONE);
        imageExecutor.execute(() -> {
            try {
                List<String> evidence = OnlinePhotoRecognition.recognize(image);
                if (!isAdded()) return;
                List<PhotoCandidate> matches = rankCandidatesFromEvidence(evidence, 0.65, true);
                if (matches.isEmpty()) {
                    Log.i(TAG, "Online recognition returned no matching waste item");
                    requireActivity().runOnUiThread(() -> {
                        analyzeLocally(image);
                    });
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    if (isAdded()) showAnalysisResults(matches);
                });
            } catch (Exception error) {
                Log.w(TAG, "Online photo recognition unavailable", error);
                if (isAdded()) requireActivity().runOnUiThread(() -> analyzeLocally(image));
            }
        });
    }

    private void analyzeLocally(Bitmap image) {
        InputImage input = InputImage.fromBitmap(image, 0);
        Task<List<ImageLabel>> labels = labeler.process(input);
        Task<Text> text = recognizer.process(input);
        Tasks.whenAllComplete(labels, text).addOnCompleteListener(done -> {
            if (!isAdded()) return;
            setBusy(false);
            if (!labels.isSuccessful() && !text.isSuccessful()) {
                showFailure();
                return;
            }
            List<ImageLabel> labelValues = labels.isSuccessful() ? labels.getResult() : Collections.emptyList();
            Text textValue = text.isSuccessful() ? text.getResult() : null;
            List<PhotoCandidate> matches = rankCandidates(labelValues, textValue);
            showAnalysisResults(matches);
        });
    }

    private void showAnalysisResults(List<PhotoCandidate> matches) {
        setBusy(false);
        if (matches.isEmpty()) {
            hint.setVisibility(View.VISIBLE);
            hint.setText(InfoPageText.get(requireContext(), "photosearchview.002") + "\n"
                    + InfoPageText.get(requireContext(), "photosearchview.007"));
        } else {
            hint.setVisibility(View.GONE);
            resultsHeading.setVisibility(View.VISIBLE);
            resultsNote.setVisibility(View.VISIBLE);
            showCandidates(matches);
        }
    }

    private List<PhotoCandidate> rankCandidates(List<ImageLabel> labels, Text recognizedText) {
        List<String> evidence = new ArrayList<>();
        if (recognizedText != null) {
            for (Text.TextBlock block : recognizedText.getTextBlocks()) {
                for (Text.Line line : block.getLines()) {
                    String value = normalize(line.getText());
                    if (value.length() >= 3) evidence.add(value);
                }
            }
        }
        for (ImageLabel label : labels) {
            if (label.getConfidence() >= 0.20f) {
                String value = normalize(label.getText());
                if (value.length() >= 4) {
                    evidence.add(value);
                    evidence.addAll(labelAliases(value));
                }
            }
        }
        return rankCandidatesFromEvidence(evidence, 0.70);
    }

    private List<PhotoCandidate> rankCandidatesFromEvidence(List<String> evidence, double threshold) {
        return rankCandidatesFromEvidence(evidence, threshold, false);
    }

    private List<PhotoCandidate> rankCandidatesFromEvidence(List<String> evidence, double threshold, boolean online) {
        if (evidence.isEmpty() || trashDB.trashItems == null) return Collections.emptyList();
        List<PhotoCandidate> results = new ArrayList<>();
        boolean useEnglish = LanguageManager.usesNonDanishContent(requireContext());
        for (TrashDB.TrashItem item : trashDB.trashItems) {
            if (item.product == null || item.sorting == null || item.sorting.isEmpty()) continue;
            double best = 0;
            List<String> names = item.getRecognitionNames();
            for (String phrase : evidence) {
                String normalized = normalize(phrase);
                for (String name : names) {
                    String candidate = normalize(name);
                    double score = online ? onlineMatch(normalized, candidate) : match(normalized, candidate);
                    best = Math.max(best, score);
                }
            }
            if (best >= threshold) results.add(new PhotoCandidate(item.getDisplayProduct(useEnglish), best));
        }
        results.sort((a, b) -> Double.compare(b.score, a.score));
        List<PhotoCandidate> distinct = new ArrayList<>();
        for (PhotoCandidate item : results) {
            if (distinct.size() >= 3 || (!distinct.isEmpty() && item.score < distinct.get(0).score - 0.15)) break;
            if (distinct.stream().noneMatch(other -> other.name.equalsIgnoreCase(item.name))) distinct.add(item);
        }
        return distinct;
    }

    private static String normalize(String text) {
        if (text == null) return "";
        return java.text.Normalizer.normalize(text.toLowerCase(Locale.ROOT), java.text.Normalizer.Form.NFKC)
                .replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
    }

    private static double match(String evidence, String product) {
        if (evidence.isEmpty() || product.isEmpty()) return 0;
        if (evidence.equals(product)) return 1;
        if (evidence.length() >= 5 && product.contains(evidence))
            return evidence.contains(" ") ? 0.82 : 0.62;
        if (product.length() >= 5 && evidence.contains(product)) return 0.88;
        String[] words = evidence.split(" ");
        if (words.length >= 2) {
            int shared = 0;
            for (String word : words) if (word.length() >= 4 && product.contains(word)) shared++;
            if (shared >= 2 && shared == words.length) return 0.72;
        }
        return 0;
    }

    private static double onlineMatch(String evidence, String product) {
        if (evidence.length() < 3 || product.length() < 2) return 0;
        if (evidence.equals(product)) return 1;
        if (evidence.contains(product) && product.length() >= 4) return 0.92;
        if (product.contains(evidence) && evidence.length() >= 4) return 0.84;
        java.util.Set<String> evidenceWords = new java.util.HashSet<>(java.util.Arrays.asList(evidence.split(" ")));
        java.util.Set<String> productWords = new java.util.HashSet<>(java.util.Arrays.asList(product.split(" ")));
        evidenceWords.retainAll(productWords);
        if (evidenceWords.isEmpty()) return 0;
        int maxWords = Math.max(evidence.split(" ").length, product.split(" ").length);
        return 0.58 + 0.25 * evidenceWords.size() / maxWords;
    }

    private static List<String> labelAliases(String label) {
        switch (label) {
            case "cellular telephone":
            case "mobile device": return java.util.Arrays.asList("mobile phone", "smartphone");
            case "carton": return java.util.Arrays.asList("cardboard box", "drink carton");
            case "tin": return Collections.singletonList("tin can");
            case "computer keyboard": return Collections.singletonList("keyboard");
            case "lightbulb": return Collections.singletonList("light bulb");
            default: return Collections.emptyList();
        }
    }

    private void showCandidates(List<PhotoCandidate> matches) {
        boolean useEnglish = LanguageManager.usesNonDanishContent(requireContext());
        String municipality = SavedMunicipalityManager.getSavedMunicipalityName(requireContext());
        for (PhotoCandidate candidate : matches) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(dp(12), dp(12), dp(12), dp(12));
            row.setBackgroundResource(R.drawable.search_group_background);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(70));
            rowParams.topMargin = dp(7);
            ImageView pictogram = new ImageView(requireContext());
            pictogram.setLayoutParams(new LinearLayout.LayoutParams(dp(46), dp(46)));
            pictogram.setScaleType(ImageView.ScaleType.FIT_CENTER);
            GradientDrawable roundedPictogram = new GradientDrawable();
            roundedPictogram.setColor(0xFFFFFFFF);
            roundedPictogram.setCornerRadius(dp(8));
            pictogram.setBackground(roundedPictogram);
            pictogram.setClipToOutline(true);
            trashDB.searchProductJson(candidate.name, useEnglish, municipality, sorting -> {
                if (!sorting.isEmpty()) {
                    String category = sorting.keySet().iterator().next();
                    pictogram.setImageResource(TrashDB.getImageResourceForKey(category, useEnglish));
                }
            });
            TextView name = new TextView(requireContext());
            name.setText(candidate.name);
            name.setTextColor(getResources().getColor(R.color.text_color, null));
            name.setTextSize(16);
            name.setPadding(dp(12), 0, 0, 0);
            row.addView(pictogram);
            row.addView(name);
            row.setOnClickListener(view -> {
                if (!recordedSelection) {
                    AchievementStore.get(requireContext()).recordPhotoSearch();
                    recordedSelection = true;
                }
                Bundle args = new Bundle();
                args.putString("initialQuery", candidate.name);
                Navigation.findNavController(view).navigate(R.id.search_trash_fragment, args);
            });
            candidatesContainer.addView(row, rowParams);
        }
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private void loadReplacementPhoto(Uri uri) {
        setBusy(true);
        imageExecutor.execute(() -> decodeImage(uri));
    }

    private void openCameraForReplacement() {
        try {
            File directory = new File(requireContext().getCacheDir(), "photo_search");
            if (!directory.exists() && !directory.mkdirs()) throw new java.io.IOException("Cannot create camera directory");
            File image = File.createTempFile("capture_", ".jpg", directory);
            pendingCameraUri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".fileprovider", image);
            takeReplacementPhoto.launch(pendingCameraUri);
        } catch (Exception error) {
            Log.e(TAG, "Could not open camera", error);
            Toast.makeText(requireContext(), InfoPageText.get(requireContext(), "photosearchview.008"), Toast.LENGTH_LONG).show();
        }
    }

    private void setBusy(boolean busy) {
        loadingIndicator.setVisibility(busy ? View.VISIBLE : View.GONE);
        photoSelection.setEnabled(!busy);
        analyzeSelection.setEnabled(!busy);
        analyzeFullImage.setEnabled(!busy);
        replacePhotoButton.setEnabled(!busy);
    }
    private void showFailure() {
        setBusy(false);
        hint.setVisibility(View.VISIBLE);
        hint.setText(InfoPageText.get(requireContext(), "photosearchview.008"));
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        bitmap = null;
    }

    @Override public void onDestroy() {
        super.onDestroy();
        labeler.close();
        recognizer.close();
        imageExecutor.shutdownNow();
    }

    private static class PhotoCandidate {
        final String name;
        final double score;
        PhotoCandidate(String name, double score) { this.name = name; this.score = score; }
    }
}
