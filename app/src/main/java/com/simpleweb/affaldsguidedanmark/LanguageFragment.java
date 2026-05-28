package com.simpleweb.affaldsguidedanmark;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class LanguageFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_language, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        NativeAdHelper.loadNativeAd(requireContext(), view);

        RadioButton danishButton = view.findViewById(R.id.danishLanguageButton);
        RadioButton englishButton = view.findViewById(R.id.englishLanguageButton);

        if (LanguageManager.isEnglish(requireContext())) {
            englishButton.setChecked(true);
        } else {
            danishButton.setChecked(true);
        }

        danishButton.setOnClickListener(v -> {
            LanguageManager.saveLanguage(requireContext(), LanguageManager.DANISH);
            requireActivity().recreate();
        });
        englishButton.setOnClickListener(v -> {
            LanguageManager.saveLanguage(requireContext(), LanguageManager.ENGLISH);
            requireActivity().recreate();
        });
    }
}
