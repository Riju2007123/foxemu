package com.fox.emu.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.fox.emu.R
import com.fox.emu.setup.SetupManager
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        val startBtn = view.findViewById<Button>(R.id.btn_start_setup)
        val progress = view.findViewById<ProgressBar>(R.id.setup_progress)
        val status = view.findViewById<TextView>(R.id.setup_status)

        startBtn.setOnClickListener {
            val setup = SetupManager(requireContext())
            startBtn.isEnabled = false
            progress.progress = 0

            lifecycleScope.launch {
                try {
                    setup.runSetup { step, pct ->
                        activity?.runOnUiThread {
                            status.text = step.label
                            progress.progress = (pct * 100).toInt()
                        }
                    }
                    status.text = "Setup complete — ready to install games"
                } catch (e: Exception) {
                    status.text = "Setup failed: ${e.message}"
                } finally {
                    activity?.runOnUiThread { startBtn.isEnabled = true }
                }
            }
        }
        return view
    }
}
