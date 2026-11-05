package com.fox.emu.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.fox.emu.R
import com.fox.emu.store.ItchClient
import com.fox.emu.store.OwnedKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StoreFragment : Fragment() {

    private var ownedGames: List<OwnedKey> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_store, container, false)
        val keyInput = view.findViewById<EditText>(R.id.itch_api_key)
        val loadBtn = view.findViewById<Button>(R.id.btn_load_library)
        val list = view.findViewById<ListView>(R.id.store_list)

        val prefs = requireContext().getSharedPreferences("foxemu", Context.MODE_PRIVATE)
        prefs.getString("itch_api_key", null)?.let { keyInput.setText(it) }

        loadBtn.setOnClickListener {
            val key = keyInput.text.toString().trim()
            if (key.isEmpty()) {
                Toast.makeText(context, "Paste your itch.io API key first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            prefs.edit().putString("itch_api_key", key).apply()

            lifecycleScope.launch {
                val games = withContext(Dispatchers.IO) {
                    try { ItchClient(key).fetchOwnedGames() } catch (e: Exception) { emptyList() }
                }
                ownedGames = games
                list.adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_list_item_1,
                    games.map { it.game.title }
                )
                if (games.isEmpty()) {
                    Toast.makeText(context, "No owned games found (check API key)", Toast.LENGTH_SHORT).show()
                }
            }
        }

        list.setOnItemClickListener { _, _, position, _ ->
            val entry = ownedGames.getOrNull(position) ?: return@setOnItemClickListener
            // Hand off to DownloadsFragment logic: fetch uploads for this game,
            // filter by platform, enqueue. Wired via a shared ViewModel in a
            // full build — kept direct here for clarity.
            Toast.makeText(context, "Open ${entry.game.title} — platform picker TODO wiring", Toast.LENGTH_SHORT).show()
        }

        return view
    }
}
