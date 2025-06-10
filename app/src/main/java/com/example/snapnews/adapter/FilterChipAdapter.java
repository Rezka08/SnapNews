package com.example.snapnews.adapter;

import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.snapnews.R;
import com.example.snapnews.databinding.ItemFilterChipBinding;
import com.example.snapnews.models.FilterChip;
import java.util.List;

public class FilterChipAdapter extends RecyclerView.Adapter<FilterChipAdapter.ChipViewHolder> {
    private static final String TAG = "FilterChipAdapter";
    private List<FilterChip> filterChips;
    private OnChipClickListener onChipClickListener;
    private int selectedPosition = -1;

    public interface OnChipClickListener {
        void onChipClick(FilterChip filterChip, int position);
    }

    private boolean allowDeselection = true;

    public FilterChipAdapter(List<FilterChip> filterChips, OnChipClickListener listener, boolean autoSelectFirst, boolean allowDeselection) {
        this.filterChips = filterChips;
        this.onChipClickListener = listener;
        this.allowDeselection = allowDeselection;

        // Only auto-select first item if autoSelectFirst is true
        if (autoSelectFirst && !filterChips.isEmpty()) {
            filterChips.get(0).setSelected(true);
            selectedPosition = 0;
            Log.d(TAG, "First chip auto-selected: " + filterChips.get(0).getName());
        } else {
            // Ensure no chips are selected initially
            for (FilterChip chip : filterChips) {
                chip.setSelected(false);
            }
            selectedPosition = -1;
            Log.d(TAG, "No chips auto-selected");
        }

        Log.d(TAG, "FilterChipAdapter created with " + filterChips.size() + " chips, autoSelectFirst: " + autoSelectFirst + ", allowDeselection: " + allowDeselection);
    }

    @NonNull
    @Override
    public ChipViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemFilterChipBinding binding = ItemFilterChipBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ChipViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ChipViewHolder holder, int position) {
        FilterChip filterChip = filterChips.get(position);
        holder.bind(filterChip, position);
    }

    @Override
    public int getItemCount() {
        return filterChips.size();
    }

    public void setSelectedPosition(int position) {
        Log.d(TAG, "Setting selected position from " + selectedPosition + " to " + position);

        // Unselect previous item
        if (selectedPosition >= 0 && selectedPosition < filterChips.size()) {
            filterChips.get(selectedPosition).setSelected(false);
        }

        // Select new item
        selectedPosition = position;
        if (selectedPosition >= 0 && selectedPosition < filterChips.size()) {
            filterChips.get(selectedPosition).setSelected(true);
            Log.d(TAG, "Selected chip: " + filterChips.get(selectedPosition).getName());
        }

        notifyDataSetChanged();
    }

    public void clearSelection() {
        if (selectedPosition >= 0 && selectedPosition < filterChips.size()) {
            filterChips.get(selectedPosition).setSelected(false);
        }
        selectedPosition = -1;
        notifyDataSetChanged();
        Log.d(TAG, "All selections cleared");
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    class ChipViewHolder extends RecyclerView.ViewHolder {
        private ItemFilterChipBinding binding;

        public ChipViewHolder(@NonNull ItemFilterChipBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(FilterChip filterChip, int position) {
            binding.textChip.setText(filterChip.getName());

            // Update chip appearance based on selection state
            updateChipAppearance(filterChip.isSelected());

            Log.d(TAG, "Binding chip: " + filterChip.getName() +
                    ", Selected: " + filterChip.isSelected() +
                    ", Position: " + position);

            // Set click listener
            binding.chipContainer.setOnClickListener(v -> {
                Log.d(TAG, "Chip clicked: " + filterChip.getName() +
                        ", Currently selected: " + filterChip.isSelected() +
                        ", Allow deselection: " + allowDeselection);

                if (onChipClickListener != null) {
                    if (filterChip.isSelected()) {
                        // Currently selected chip was clicked
                        if (allowDeselection) {
                            // Allow deselection - clear all selections
                            clearSelection();
                            Log.d(TAG, "Chip deselected: " + filterChip.getName());
                        } else {
                            // Don't allow deselection - keep current selection
                            Log.d(TAG, "Deselection not allowed for: " + filterChip.getName());
                            return; // Don't notify listener
                        }
                    } else {
                        // Not selected chip was clicked - select it
                        setSelectedPosition(position);
                        Log.d(TAG, "Chip selected: " + filterChip.getName());
                    }

                    // Notify listener
                    onChipClickListener.onChipClick(filterChip, position);
                }
            });
        }

        private void updateChipAppearance(boolean isSelected) {
            GradientDrawable background = new GradientDrawable();
            background.setCornerRadius(50f);

            if (isSelected) {
                // Selected state
                background.setColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.primary_color));
                background.setStroke(2, ContextCompat.getColor(binding.getRoot().getContext(), R.color.primary_color));
                binding.textChip.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.white));
                Log.d(TAG, "Chip appearance set to SELECTED");
            } else {
                // Unselected state
                background.setColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.surface_color));
                background.setStroke(2, ContextCompat.getColor(binding.getRoot().getContext(), R.color.gray));
                binding.textChip.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.on_surface));
                Log.d(TAG, "Chip appearance set to UNSELECTED");
            }

            binding.chipContainer.setBackground(background);
        }
    }
}