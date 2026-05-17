package com.simpleweb.affaldsguidedanmark;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class TrashTypesFragment extends Fragment {

    private RecyclerView recyclerView;
    private View emptyStateView;
    private TrashTypeAdapter trashTypeAdapter;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trash_types, container, false);

        recyclerView = view.findViewById(R.id.listTrashTypes);
        emptyStateView = view.findViewById(R.id.trashTypesEmptyText);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        trashTypeAdapter = new TrashTypeAdapter(new ArrayList<>(), new TrashTypeAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(TrashType trashType) {
                Bundle args = new Bundle();
                args.putParcelable("trashType", trashType);
                args.putString("selectedTrashGroup", trashType.getNavn());
                Navigation.findNavController(view).navigate(R.id.fragment_trash_type_details, args);
            }
        });

        recyclerView.setAdapter(trashTypeAdapter);

        TrashDB trashDB = new TrashDB(getResources());
        List<TrashType> trashTypes = trashDB.getLocalTrashTypes(LanguageManager.isEnglish(requireContext()));
        trashTypeAdapter.setData(trashTypes);
        updateEmptyState(trashTypes.isEmpty());

        NativeAdHelper.loadNativeAd(requireContext(), view);

        return view;
    }

    private void updateEmptyState(boolean isEmpty) {
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        emptyStateView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

}
