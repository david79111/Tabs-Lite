package com.gbros.tabslite.adapters
import android.content.Context
import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ListAdapter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import com.gbros.tabslite.SearchResultFragment.Callback
import com.gbros.tabslite.ViewPlaylistFragmentDirections
import com.gbros.tabslite.data.AppDatabase
import com.gbros.tabslite.data.PlaylistEntry
import com.gbros.tabslite.databinding.ListItemPlaylistTabBinding
import com.gbros.tabslite.utilities.TabHelper
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

private const val LOG_NAME = "tabslite.MyPlaylistEntr"

/**
 * [RecyclerView.Adapter] that can display a [PlaylistEntry] and makes a call to the
 * specified [Callback].
 */
class MyPlaylistEntryRecyclerViewAdapter(private val context: Context, private val playlistName: String) : ListAdapter<PlaylistEntry, RecyclerView.ViewHolder>(PlaylistEntryCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ListItemPlaylistTabBinding.inflate(LayoutInflater.from(parent.context), parent,false)
        return ViewHolder(view, context, playlistName)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        (holder as ViewHolder).bind(item)
    }

    class ViewHolder(
            private val binding: ListItemPlaylistTabBinding,
            private val context: Context,
            private val playlistName: String
    ) : RecyclerView.ViewHolder(binding.root) {
        fun setClickListener(){
            binding.setClickListener {
                binding.tab?.let { tab ->
                    binding.entry?.let { playlistEntry ->
                        // navigate to the tab detail page on item click
                        Log.d(LOG_NAME, "Navigating from Playlist to Tab ${tab.tabId}")
                        val direction = ViewPlaylistFragmentDirections.actionPlaylistFragmentToTabDetailFragment2(true, playlistName, tab.tabId, playlistEntry)
                        binding.root.findNavController().navigate(direction)
                    }
                }
            }
        }

        fun bind(item: PlaylistEntry) {
            binding.apply {
                entry = item

                // get tab from internet
                val getDataJob = GlobalScope.async { TabHelper.fetchTabFromInternet(item.tabId, AppDatabase.getInstance(context)) }
                getDataJob.invokeOnCompletion {
                    if (getDataJob.getCompleted()) {
                        // get tab from db
                        val getTabJob = GlobalScope.async { AppDatabase.getInstance(context).tabFullDao().getTab(item.tabId) }
                        getTabJob.invokeOnCompletion {
                            val tab = getTabJob.getCompleted()

                            // bind tab
                            binding.tab = tab
                            binding.version = "ver. ${tab?.version}"
                        }
                    } else {
                        Log.e(LOG_NAME, "Could not fetch tab for playlist listing.  Check internet connection?")
                    }
                }
                setClickListener()
                executePendingBindings()
            }
        }
    }
}

private class PlaylistEntryCallback : DiffUtil.ItemCallback<PlaylistEntry>() {

    override fun areItemsTheSame(oldItem: PlaylistEntry, newItem: PlaylistEntry): Boolean {
        return oldItem.entryId == newItem.entryId
    }

    override fun areContentsTheSame(oldItem: PlaylistEntry, newItem: PlaylistEntry): Boolean {
        return oldItem.entryId == newItem.entryId
    }
}