package com.injector.loader.ui

import android.app.Dialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.injector.loader.core.FileItem
import com.injector.loader.core.FileManager
import java.io.File

class FilePickerDialog(val onSelect: (String) -> Unit) : AppCompatDialogFragment() {
    private val fm = FileManager()
    private var path = "/sdcard"
    private lateinit var rv: RecyclerView
    private lateinit var tv: TextView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        tv = TextView(requireContext()).apply {
            text = path
            textSize = 12f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 8 }
        }

        val btnBack = Button(requireContext()).apply {
            text = "返回"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 8 }
            setOnClickListener {
                path = File(path).parent ?: return@setOnClickListener
                refresh()
            }
        }

        rv = RecyclerView(requireContext()).apply {
            layoutManager = LinearLayoutManager(context)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 400
            )
        }

        root.addView(tv)
        root.addView(btnBack)
        root.addView(rv)
        refresh()

        return AlertDialog.Builder(requireContext())
            .setTitle("选择 SO 文件")
            .setView(root)
            .setNegativeButton("取消", null)
            .create()
    }

    private fun refresh() {
        tv.text = path
        val items = fm.getSOFiles(path)
        rv.adapter = FileAdapter(items) { item ->
            if (item.isDir) {
                path = item.path
                refresh()
            } else {
                onSelect(item.path)
                dismiss()
            }
        }
    }

    private class FileAdapter(val items: List<FileItem>, val cb: (FileItem) -> Unit) :
        RecyclerView.Adapter<FileAdapter.VH>() {
        inner class VH(v: android.view.View) : RecyclerView.ViewHolder(v) {
            fun bind(item: FileItem) {
                v.findViewById<TextView>(android.R.id.text1).text = item.name
                v.findViewById<TextView>(android.R.id.text2).text =
                    if (item.isDir) "[文件夹]" else FileManager().formatSize(item.size)
                v.setOnClickListener { cb(item) }
            }
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, t: Int): VH {
            val v = LinearLayout(parent.context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT, 60
                )
                setPadding(8, 8, 8, 8)
            }
            val t1 = TextView(parent.context).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                textSize = 14f
            }
            val t2 = TextView(parent.context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginStart = 8 }
                textSize = 12f
            }
            v.addView(t1)
            v.addView(t2)
            return VH(v)
        }

        override fun getItemCount() = items.size
        override fun onBindViewHolder(vh: VH, i: Int) = vh.bind(items[i])
    }
}
