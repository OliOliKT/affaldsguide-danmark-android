package com.simpleweb.affaldsguidedanmark;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MunicipalitiesFragment extends Fragment {
    private MunicipalityAdapter municipalityAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_municipalities, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        NativeAdHelper.loadNativeAd(requireContext(), view);

        MunicipalityDB municipalityDB = new MunicipalityDB(getResources());
        List<Municipality> municipalities = municipalityDB.getMunicipalities();

        RecyclerView recyclerView = view.findViewById(R.id.listMunicipalities);
        TextView emptyTextView = view.findViewById(R.id.emptyMunicipalitiesText);
        EditText searchInput = view.findViewById(R.id.searchMunicipalityInput);
        ImageView clearSearchButton = view.findViewById(R.id.clearMunicipalitySearchButton);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        municipalityAdapter = new MunicipalityAdapter(municipalities, SavedMunicipalityManager.getSavedMunicipalityName(requireContext()), municipality -> {
            Bundle args = new Bundle();
            args.putParcelable("municipality", municipality);
            Navigation.findNavController(view).navigate(R.id.fragment_municipality_details, args);
        });
        recyclerView.setAdapter(municipalityAdapter);

        emptyTextView.setVisibility(municipalities.isEmpty() ? View.VISIBLE : View.GONE);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                municipalityAdapter.filter(s.toString());
                clearSearchButton.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        clearSearchButton.setOnClickListener(clearView -> {
            searchInput.setText("");
            searchInput.requestFocus();

            InputMethodManager imm = (InputMethodManager) clearView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (municipalityAdapter != null) {
            municipalityAdapter.setSavedMunicipalityName(SavedMunicipalityManager.getSavedMunicipalityName(requireContext()));
        }
    }
}
