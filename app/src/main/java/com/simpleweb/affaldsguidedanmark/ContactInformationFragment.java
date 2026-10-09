package com.simpleweb.affaldsguidedanmark;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ContactInformationFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        final View v = inflater.inflate(R.layout.fragment_contact_information, container, false);

        InfoPageText.set(v.findViewById(R.id.contactPageTitle), "moreinfoview.011");
        InfoPageText.set(v.findViewById(R.id.contactPageBody), "moreinfoview.012");

        NativeAdHelper.loadNativeAd(requireContext(), v);

        return v;
    }

}
