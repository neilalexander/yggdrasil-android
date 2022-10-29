package eu.neilalexander.yggdrasil

import android.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONArray

class PeersActivity : AppCompatActivity() {
    private var state = PacketTunnelState
    private lateinit var config: ConfigurationProxy
    private lateinit var inflater: LayoutInflater

    private lateinit var connectedTableLayout: TableLayout
    private lateinit var connectedTableLabel: TextView
    private lateinit var configuredTableLayout: TableLayout
    private lateinit var configuredTableLabel: TextView
    private lateinit var multicastListenSwitch: Switch
    private lateinit var multicastBeaconSwitch: Switch
    private lateinit var addPeerButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_peers)

        config = ConfigurationProxy(applicationContext)
        inflater = LayoutInflater.from(this)

        connectedTableLayout = findViewById(R.id.connectedPeersTableLayout)
        connectedTableLabel = findViewById(R.id.connectedPeersLabel)

        configuredTableLayout = findViewById(R.id.configuredPeersTableLayout)
        configuredTableLabel = findViewById(R.id.configuredPeersLabel)

        multicastListenSwitch = findViewById(R.id.enableMulticastListen)
        multicastListenSwitch.setOnCheckedChangeListener { button, _ ->
            config.multicastListen = button.isChecked
        }
        multicastBeaconSwitch = findViewById(R.id.enableMulticastBeacon)
        multicastBeaconSwitch.setOnCheckedChangeListener { button, _ ->
            config.multicastBeacon = button.isChecked
        }
        multicastListenSwitch.isChecked = config.multicastListen
        multicastBeaconSwitch.isChecked = config.multicastBeacon

        addPeerButton = findViewById(R.id.addPeerButton)
        addPeerButton.setOnClickListener {
            val view = inflater.inflate(R.layout.dialog_addpeer, null)
            val input = view.findViewById<TextInputEditText>(R.id.addPeerInput)
            val builder: AlertDialog.Builder = AlertDialog.Builder(ContextThemeWrapper(this, R.style.Theme_MaterialComponents_DayNight_Dialog))
            builder.setTitle("Add Configured Peer")
            builder.setView(view)
            builder.setPositiveButton("Add") { dialog, _ ->
                config.updateJSON { json ->
                    json.getJSONArray("Peers").put(input.text.toString().trim())
                }
                dialog.dismiss()
                updateConfiguredPeers()
            }
            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            builder.show()
        }
    }

    override fun onResume() {
        super.onResume()

        updateConfiguredPeers()
        updateConnectedPeers()
    }

    private fun updateConfiguredPeers() {
        val peers = config.getJSON().getJSONArray("Peers")

        when (peers.length()) {
            0 -> {
                configuredTableLayout.visibility = View.GONE
                configuredTableLabel.text = "No peers currently configured"
            }
            else -> {
                configuredTableLayout.visibility = View.VISIBLE
                configuredTableLabel.text = "Configured Peers"

                configuredTableLayout.removeAllViewsInLayout()
                for (i in 0 until peers.length()) {
                    val peer = peers[i].toString()
                    val view = inflater.inflate(R.layout.peers_configured, null)
                    view.findViewById<TextView>(R.id.addressValue).text = peer
                    view.findViewById<ImageButton>(R.id.deletePeerButton).tag = i

                    view.findViewById<ImageButton>(R.id.deletePeerButton).setOnClickListener { button ->
                        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                        builder.setTitle("Remove ${peer}?")
                        builder.setPositiveButton("Remove") { dialog, _ ->
                            config.updateJSON { json ->
                                json.getJSONArray("Peers").remove(button.tag as Int)
                            }
                            dialog.dismiss()
                            updateConfiguredPeers()
                        }
                        builder.setNegativeButton("Cancel") { dialog, _ ->
                            dialog.cancel()
                        }
                        builder.show()
                    }
                    configuredTableLayout.addView(view)
                }
            }
        }
    }

    private fun updateConnectedPeers() {
        val peers = state.peersState ?: JSONArray("[]")

        when (peers.length()) {
            0 -> {
                connectedTableLayout.visibility = View.GONE
                connectedTableLabel.text = "No peers currently connected"
            }
            else -> {
                connectedTableLayout.visibility = View.VISIBLE
                connectedTableLabel.text = "Connected Peers"

                connectedTableLayout.removeAllViewsInLayout()
                for (i in 0 until peers.length()) {
                    val peer = peers.getJSONObject(i)
                    val view = inflater.inflate(R.layout.peers_connected, null)
                    val ip = peer.getString("IP")
                    view.findViewById<TextView>(R.id.addressLabel).text = ip
                    view.findViewById<TextView>(R.id.detailsLabel).text = peer.getString("Remote")
                    connectedTableLayout.addView(view)
                }
            }
        }
    }
}