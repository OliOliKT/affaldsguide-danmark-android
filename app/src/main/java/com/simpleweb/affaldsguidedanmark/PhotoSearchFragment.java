package com.simpleweb.affaldsguidedanmark;

import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

public class PhotoSearchFragment extends Fragment {
    private static final String TAG = "PhotoSearch";
    private final ExecutorService imageExecutor = Executors.newSingleThreadExecutor();
    private PhotoSelectionView photoSelection;
    private LinearLayout candidatesContainer;
    private TextView hint;
    private Button analyzeSelection, analyzeFullImage;
    private Bitmap bitmap;
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
        analyzeSelection = root.findViewById(R.id.analyzeSelection);
        analyzeFullImage = root.findViewById(R.id.analyzeFullImage);
        ((TextView) root.findViewById(R.id.photoTitle)).setText(InfoPageText.get(requireContext(), "photosearchview.005"));
        hint.setText(InfoPageText.get(requireContext(), "photo.objectSelectionHint"));
        analyzeSelection.setText(InfoPageText.get(requireContext(), "photo.analyzeSelection"));
        analyzeFullImage.setText(InfoPageText.get(requireContext(), "photo.useFullImage"));
        analyzeSelection.setOnClickListener(view -> {
            Bitmap cropped = photoSelection.selectedBitmap();
            if (cropped == null) {
                hint.setText(InfoPageText.get(requireContext(), "photo.objectSelectionHint"));
            } else {
                analyze(cropped);
            }
        });
        analyzeFullImage.setOnClickListener(view -> { if (bitmap != null) analyze(bitmap); });
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
        hint.setText(InfoPageText.get(requireContext(), "photosearchview.001"));
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
            if (matches.isEmpty()) {
                hint.setText(InfoPageText.get(requireContext(), "photosearchview.002") + "\n"
                        + InfoPageText.get(requireContext(), "photosearchview.007"));
            } else {
                hint.setText(InfoPageText.get(requireContext(), "photosearchview.003") + "\n"
                        + InfoPageText.get(requireContext(), "photosearchview.004"));
                showCandidates(matches);
            }
        });
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
        if (evidence.isEmpty() || trashDB.trashItems == null) return Collections.emptyList();
        List<PhotoCandidate> results = new ArrayList<>();
        boolean useEnglish = LanguageManager.usesNonDanishContent(requireContext());
        for (TrashDB.TrashItem item : trashDB.trashItems) {
            if (item.product == null || item.sorting == null || item.sorting.isEmpty()) continue;
            String english = normalize(item.productEn);
            String danish = normalize(item.product);
            double best = 0;
            for (String phrase : evidence) {
                double score = Math.max(match(phrase, english), match(phrase, danish));
                best = Math.max(best, score);
            }
            if (best >= 0.70) results.add(new PhotoCandidate(item.getDisplayProduct(useEnglish), best));
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
            row.setPadding(12, 8, 12, 8);
            row.setBackgroundResource(R.drawable.search_group_background);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(60));
            rowParams.topMargin = dp(7);
            ImageView pictogram = new ImageView(requireContext());
            pictogram.setLayoutParams(new LinearLayout.LayoutParams(dp(46), dp(46)));
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
    private void setBusy(boolean busy) {
        analyzeSelection.setEnabled(!busy);
        analyzeFullImage.setEnabled(!busy);
    }
    private void showFailure() {
        setBusy(false);
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
