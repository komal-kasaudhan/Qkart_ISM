import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.qkart.databinding.PickupProfileBinding
import android.graphics.Color
import com.example.qkart.PickupProfile


class PickupAdapter(
    private val onEdit: (PickupProfile) -> Unit,
    private val onDelete: (PickupProfile) -> Unit,
    private val onSelect: (PickupProfile) -> Unit
) : RecyclerView.Adapter<PickupAdapter.ViewHolder>() {

    private var profiles = listOf<PickupProfile>()

    fun submitList(newList: List<PickupProfile>) {
        profiles = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = PickupProfileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val profile = profiles[position]
        holder.bind(profile)
    }
    // PickupAdapter.kt ke andar ye function add karo
    fun getSelectedProfile(): PickupProfile? {
        return profiles.find { it.isSelected }
    }

    override fun getItemCount() = profiles.size

    inner class ViewHolder(val binding: PickupProfileBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(profile: PickupProfile) {
            binding.tvProfileName.text = profile.name
            binding.tvProfileHostel.text = "${profile.hostel}, Room ${profile.roomNo}"
            binding.tvProfilePhone.text = profile.phone
            binding.tvAltPhone.text = profile.altPhone

            // Selected logic (Background change logic)

            if (profile.isSelected) {
                binding.tvSelectedTag.visibility = View.VISIBLE
                binding.parentCard.strokeWidth = 5 // Border thickness
                binding.parentCard.strokeColor = Color.parseColor("#2E7D32") // Green border
            } else {
                binding.tvSelectedTag.visibility = View.GONE
                binding.parentCard.strokeWidth = 0
            }
            // Click Listeners
            binding.root.setOnClickListener { onSelect(profile) }
            binding.ivEditProfile.setOnClickListener { onEdit(profile) }
            binding.ivDeleteProfile.setOnClickListener { onDelete(profile) }
        }
    }
}