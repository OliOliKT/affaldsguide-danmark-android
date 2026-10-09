package com.simpleweb.affaldsguidedanmark;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class AboutFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        final View v = inflater.inflate(R.layout.fragment_about, container, false);

        InfoPageText.set(v.findViewById(R.id.aboutBackgroundTitle), "moreinfoview.007");
        InfoPageText.set(v.findViewById(R.id.aboutBackgroundBody), "moreinfoview.008");
        InfoPageText.set(v.findViewById(R.id.aboutSortingTitle), "moreinfoview.025");
        InfoPageText.set(v.findViewById(R.id.aboutSortingBody), "moreinfoview.026");
        InfoPageText.set(v.findViewById(R.id.aboutHowTitle), "moreinfoview.009");
        InfoPageText.set(v.findViewById(R.id.aboutHowBody), "android.about.how.body");

        NativeAdHelper.loadNativeAd(requireContext(), v);

        return v;
    }

}
