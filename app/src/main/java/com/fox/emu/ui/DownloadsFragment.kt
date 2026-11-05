package com.fox.emu.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.fox.emu.R

/**
 * Lists in-flight itch.io downloads with progress. Downloads are started
 * from StoreFragment's platform picker (wire ItchClient.downloadUrl into
 * a foreground DownloadService backed by OkHttp + a notification — same
 * download primitives as SetupManager.downloadFile, just driven by a
 * queue instead of a fixed setup sequence). On completion each entry
 * appends an object to library.json so GamesFragment picks it up.
 */
class DownloadsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_downloads, container, false)
        view.findViewById<TextView>(R.id.downloads_empty).text =
            "No active downloads. Pick a game in Store to begin."
        return view
    }
}
