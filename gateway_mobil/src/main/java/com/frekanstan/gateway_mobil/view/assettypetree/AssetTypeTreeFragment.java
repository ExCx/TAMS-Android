package com.frekanstan.gateway_mobil.view.assettypetree;

import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.frekanstan.asset_management.view.shared.ICanScanCode;
import com.frekanstan.asset_management.view.shared.ISearchableListFragment;
import com.frekanstan.gateway_mobil.R;
import com.frekanstan.gateway_mobil.app.assets.AssetDAO;
import com.frekanstan.gateway_mobil.app.assettypes.AssetTypeDAO;
import com.frekanstan.gateway_mobil.app.assettypes.AssetTypeRepository;
import com.frekanstan.gateway_mobil.data.Asset_;
import com.frekanstan.gateway_mobil.view.MainActivity;
import com.google.common.base.Strings;

import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import lombok.Getter;
import lombok.val;
import lombok.var;
import tellh.com.recyclertreeview_lib.TreeNode;
import tellh.com.recyclertreeview_lib.TreeViewAdapter;

@SuppressWarnings("rawtypes")
public class AssetTypeTreeFragment extends Fragment implements ISearchableListFragment, ICanScanCode {
    private MainActivity context;
    private String query = "";
    private RecyclerView mRecyclerView;
    private CompositeDisposable compositeDisposable;
    private SearchView searchView;

    @Getter
    public static long highlightedId = -1;

    @Override
    public String getQuery() {
        return query;
    }

    @Override
    public void setQuery(String query) {
        this.query = query;
        refreshList();
    }

    public AssetTypeTreeFragment() { }

    public static AssetTypeTreeFragment newInstance() {
        return new AssetTypeTreeFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity)
            this.context = (MainActivity) context;
    }

    Observable<List<TreeNode>> oTree;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        val view = inflater.inflate(R.layout.asset_type_list_fragment, container, false);
        mRecyclerView = view.findViewById(R.id.asset_type_list_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                context.hideKeyboard();
            }
        });

        //tree adapter
        context.showProgBar();
        oTree = Observable.defer(() -> Observable.just(AssetTypeRepository.mAssetTypeTree));
        val subs = oTree.subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::buildTree, Throwable::printStackTrace);
        compositeDisposable = new CompositeDisposable();
        compositeDisposable.add(subs);
        context.actionButton.show();
        val typeCount = (int) AssetTypeDAO.getDao().count(new Bundle());
        context.setFooterText(String.format(context.getLocale(), context.getString(R.string.number_asset_types), typeCount));
        return view;
    }

    private void buildTree(List<TreeNode> tree) {
        val adapter = new TreeViewAdapter(
                tree,
                Collections.singletonList(new AssetTypeTreeAdapter(context, getArguments())));
        adapter.setOnTreeNodeListener(new TreeViewAdapter.OnTreeNodeListener() {
            @Override
            public boolean onClick(TreeNode treeNode, RecyclerView.ViewHolder viewHolder) {
                var assetType = ((AssetTypeLayout) treeNode.getContent()).getAssetType();
                if (treeNode.isExpand() || !treeNode.isLeaf()) {
                    context.getBinding().getRoot().performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                    onToggle(!treeNode.isExpand(), viewHolder);
                    return false;
                }
                goToAssetList(assetType.getId(), 0);
                context.showProgBar();
                val oAdapter = Observable.defer(() -> Observable.fromIterable(AssetTypeRepository.getSubBranches(assetType)));
                val subs = oAdapter.subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                treeNode::addChild,
                                Throwable::printStackTrace,
                                () -> {
                                    context.hideProgBar();
                                    if (treeNode.getChildList() == null)
                                        goToAssetList(assetType.getId(), 0);
                                    else
                                        new Handler().postDelayed(viewHolder.itemView::performClick, 500);
                                }
                        );
                compositeDisposable.add(subs);
                return false;
            }

            @Override
            public void onToggle(boolean isExpand, RecyclerView.ViewHolder viewHolder) {
                ((AssetTypeTreeAdapter.ViewHolder)viewHolder).getArrow().animate().rotationBy(isExpand ? 90 : -90).start();
            }
        });
        mRecyclerView.setAdapter(adapter);
        context.hideProgBar();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_search_only, menu);

        //arama
        val searchManager = (SearchManager) context.getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setInputType(InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        if (searchManager != null)
            searchView.setSearchableInfo(searchManager.getSearchableInfo(context.getComponentName()));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.length() == 0)
                    setQuery(newText);
                else if (!searchView.getQuery().toString().equals(newText.toUpperCase()))
                    searchView.setQuery(newText.toUpperCase(), false);
                return false;
            }
        });
        searchView.setIconifiedByDefault(true);
        if (!Strings.isNullOrEmpty(getQuery())) {
            searchView.setIconified(false);
            searchView.setQuery(getQuery(), false);
            searchView.clearFocus();
        }
    }

    @Override
    public void refreshList() {
        context.showProgBar();
        AssetTypeRepository.filterAssetTypeTree(getQuery());
        val subs = oTree.subscribe(this::buildTree);
        context.setFooterText(String.format(context.getLocale(), context.getString(R.string.number_asset_types), AssetTypeRepository.mAssetTypeTree.size()));
        compositeDisposable.add(subs);
    }

    @Override
    public void onCodeScanned(String code) {
        val asset = AssetDAO.getDao().getBox().query().equal(Asset_.rfidCode, code).build().findFirst();
        if (asset == null)
            Toast.makeText(context, R.string.foreign_asset_qrcode_read, Toast.LENGTH_SHORT).show();
        else
            goToAssetList(asset.getAssetTypeId(), asset.getId());
    }

    @Override
    public void onResume() {
        super.onResume();
        context.showHideFooter(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        compositeDisposable.dispose();
    }

    private void goToAssetList(long assetTypeId, long assetId) {
        var bundle = requireArguments();
        bundle.putLong("assetTypeId", assetTypeId);
        if (assetId != 0)
            bundle.putLong("assetId", assetId);
        switch (bundle.getString("operation", "")) {
            case "counting":
                context.nav.navigate(R.id.countingTabsFragment, bundle);
                break;
            case "labeling":
                context.nav.navigate(R.id.labelingTabsFragment, bundle);
                break;
        }
    }
}