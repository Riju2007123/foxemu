package com.fox.emu.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.fox.emu.R
import com.fox.emu.container.ContainerManager
import com.fox.emu.container.GameEntry
import com.fox.emu.container.LaunchRouter
import org.json.JSONArray
import java.io.File

class GamesFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_games, container, false)
        val list = view.findViewById<ListView>(R.id.games_list)

        val games = loadLibrary()
        list.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            games.map { "${it.title} [${it.platform}]" }
        )

        val router = LaunchRouter(ContainerManager(requireContext()))
        list.setOnItemClickListener { _, _, position, _ ->
            val game = games.getOrNull(position) ?: return@setOnItemClickListener
            try {
                router.launch(game)
            } catch (e: Exception) {
                Toast.makeText(context, "Launch failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
        return view
    }

    /** library.json is written by DownloadsFragment once a download completes. */
    private fun loadLibrary(): List<GameEntry> {
        val file = File(requireContext().filesDir, "library.json")
        if (!file.exists()) return emptyList()
        val arr = JSONArray(file.readText())
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            GameEntry(
                title = o.getString("title"),
                platform = com.fox.emu.container.Platform.valueOf(o.getString("platform")),
                executablePath = o.getString("path"),
                containerId = o.getString("containerId")
            )
        }
    }
}
