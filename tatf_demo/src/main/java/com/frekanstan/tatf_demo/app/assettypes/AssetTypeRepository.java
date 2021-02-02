package com.frekanstan.tatf_demo.app.assettypes;

import android.os.AsyncTask;
import android.os.Bundle;

import com.frekanstan.tatf_demo.app.assets.AssetDAO;
import com.frekanstan.tatf_demo.data.AssetType;
import com.frekanstan.tatf_demo.view.assettypetree.AssetTypeLayout;
import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import lombok.Setter;
import lombok.val;
import lombok.var;
import tellh.com.recyclertreeview_lib.TreeNode;

@SuppressWarnings("rawtypes")
public class AssetTypeRepository
{
    public static List<TreeNode> mAssetTypeTree;

    @Setter
    private static long[] availableTypeIds;

    static long[] getAvailableTypeIds() {
        if (availableTypeIds == null)
            availableTypeIds = AssetDAO.getDao().getAllAvailableTypeIds();
        return availableTypeIds;
    }

    public static List<TreeNode> initialTree;

    public static void buildInitialTree() {
        if (initialTree == null) {
            try {
                initialTree = new AssetTypeTreeMaker().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR).get();
                mAssetTypeTree = initialTree;
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void filterAssetTypeTree(String query) {
        try {
            if (Strings.isNullOrEmpty(query))
                mAssetTypeTree = initialTree;
            else
                mAssetTypeTree = new AssetTypeTreeMakerSearch().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, query).get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static class AssetTypeTreeMaker extends AsyncTask<String, Integer, ArrayList<TreeNode>> {
        @Override
        protected ArrayList<TreeNode> doInBackground(String... info) {
            val nodes = new ArrayList<TreeNode>();
            Bundle input = new Bundle();
            input.putByte("hasAssets", (byte)1);
            input.putInt("depth", 1);
            val assetDAO = AssetDAO.getDao();
            for (var type : AssetTypeDAO.getDao().getAll(input)) {
                val count = assetDAO.countByAssetCode(type.getAssetCode(), false, false);
                val countedCount = assetDAO.countByAssetCode(type.getAssetCode(), true, false);
                val labeledCount = assetDAO.countByAssetCode(type.getAssetCode(), false, true);
                val node = new TreeNode<>(new AssetTypeLayout(type, count, countedCount, labeledCount));
                input.putInt("depth", 2);
                input.putString("assetCode", type.getAssetCode());
                for (var type2 : AssetTypeDAO.getDao().getAll(input)) {
                    val count2 = assetDAO.countByAssetCode(type2.getAssetCode(), false, false);
                    val countedCount2 = assetDAO.countByAssetCode(type2.getAssetCode(), true, false);
                    val labeledCount2 = assetDAO.countByAssetCode(type2.getAssetCode(), false, true);
                    val node2 = new TreeNode<>(new AssetTypeLayout(type2, count2, countedCount2, labeledCount2));
                    node.addChild(node2);
                }
                nodes.add(node);
            }
            return nodes;
        }
    }

    public static class AssetTypeTreeMakerSearch extends AsyncTask<String, Integer, ArrayList<TreeNode>> {
        @Override
        protected ArrayList<TreeNode> doInBackground(String... info) {
            val nodes = new ArrayList<TreeNode>();
            Bundle input = new Bundle();
            input.putByte("hasAssets", (byte)1);
            input.putString("query", info[0]);
            val assetDAO = AssetDAO.getDao();
            val allTypes = AssetTypeDAO.getDao().getAll(input);
            var addedTypeIds = new ArrayList<Long>();
            for (var type : allTypes) {
                val ancestorsAndSelf = type.getAncestorsAndSelf();
                ancestorsAndSelf.add(type);
                Collections.sort(ancestorsAndSelf, (u1, u2) -> Integer.compare(u2.getDepth(), u1.getDepth()));
                TreeNode lastNode = null;
                for (var t : ancestorsAndSelf)
                {
                    if (!addedTypeIds.contains(t.getId())) {
                        addedTypeIds.add(t.getId());
                        val count = assetDAO.countByAssetCode(t.getAssetCode(), false, false);
                        val countedCount = assetDAO.countByAssetCode(t.getAssetCode(), true, false);
                        val labeledCount = assetDAO.countByAssetCode(t.getAssetCode(), false, true);
                        val node = new TreeNode<>(new AssetTypeLayout(t, count, countedCount, labeledCount));
                        if (lastNode != null)
                            node.addChild(lastNode);
                        lastNode = node;
                        nodes.add(node);
                    } else if (lastNode != null){
                        for (var n : nodes) {
                            if (((AssetTypeLayout) n.getContent()).getAssetType().getAssetCode().equals(t.getAssetCode())) {
                                n.addChild(lastNode);
                                break;
                            }
                        }
                    }
                }
            }
            return nodes;
        }
    }

    public static ArrayList<TreeNode> getSubBranches(AssetType type) {
        val nodes = new ArrayList<TreeNode>();
        Bundle input = new Bundle();
        input.putInt("depth", type.getDepth() + 1);
        input.putString("assetCode", type.getAssetCode());
        input.putByte("hasAssets", (byte)1);
        val assetDAO = AssetDAO.getDao();
        for (var subtype : AssetTypeDAO.getDao().getAll(input)) {
            val count = assetDAO.countByAssetCode(subtype.getAssetCode(), false, false);
            val countedCount = assetDAO.countByAssetCode(subtype.getAssetCode(), true, false);
            val labeledCount = assetDAO.countByAssetCode(subtype.getAssetCode(), false, true);
            nodes.add(new TreeNode<>(new AssetTypeLayout(subtype, count, countedCount, labeledCount)));
        }
        return nodes;
    }
}
