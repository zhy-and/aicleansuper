package com.example.cleansuperai

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.WindowCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.example.cleansuperai.databinding.ActivityMainBinding
import com.example.cleansuperai.ui.compress.CompressFragment
import com.example.cleansuperai.ui.home.HomeFragment
import com.example.cleansuperai.ui.profile.ProfileFragment
import com.example.cleansuperai.ui.swipe.SwipeFragment
import com.example.cleansuperai.ui.tools.ToolsFragment

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var systemBottomInset = 0
//
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val bottomPadding = binding.bottomNavigation.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            systemBottomInset = systemBars.bottom
            v.setPadding(0, systemBars.top, 0, 0)
            binding.bottomNavigation.setPadding(
                binding.bottomNavigation.paddingLeft,
                binding.bottomNavigation.paddingTop,
                binding.bottomNavigation.paddingRight,
                bottomPadding + systemBars.bottom,
            )
            applyDetailChrome()
            insets
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.menu_home -> HomeFragment()
                R.id.menu_swipe -> SwipeFragment()
                R.id.menu_compress -> CompressFragment()
                R.id.menu_tools -> ToolsFragment()

                else -> null
            }
            fragment?.let {
                supportFragmentManager.popBackStack(
                    null,
                    androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE,
                )
                showFragment(it, item.itemId.toString())
                true
            } ?: false
        }

        supportFragmentManager.addOnBackStackChangedListener {
            applyDetailChrome()
        }

        if (savedInstanceState == null) {
            binding.bottomNavigation.selectedItemId = R.id.menu_home
        }
    }

    /**
     * Detail pages hide the bottom nav. Re-anchor the fragment container to the parent bottom
     * and apply the system gesture/nav inset so NestedScrollView / RecyclerView get a bounded
     * viewport shorter than their content (otherwise tall pages cannot scroll).
     */
    private fun applyDetailChrome() {
        val showingDetail = supportFragmentManager.backStackEntryCount > 0
        binding.bottomNavigation.isVisible = !showingDetail

        val params = binding.fragmentContainer.layoutParams as ConstraintLayout.LayoutParams
        if (showingDetail) {
            params.bottomToTop = ConstraintLayout.LayoutParams.UNSET
            params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            binding.fragmentContainer.updatePadding(bottom = systemBottomInset)
        } else {
            params.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
            params.bottomToTop = binding.bottomNavigation.id
            binding.fragmentContainer.updatePadding(bottom = 0)
        }
        binding.fragmentContainer.layoutParams = params
    }

    fun selectDestination(itemId: Int) {
        supportFragmentManager.popBackStack(
            null,
            androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE,
        )
        binding.bottomNavigation.selectedItemId = itemId
    }

    fun openProfile() {
        openDetail(ProfileFragment(), "profile")
    }

    fun openDetail(fragment: Fragment, tag: String) {
        supportFragmentManager.commit {
            replace(R.id.fragmentContainer, fragment, tag)
            addToBackStack(tag)
        }
    }

    private fun showFragment(fragment: Fragment, tag: String) {
        supportFragmentManager.commit {
            replace(R.id.fragmentContainer, fragment, tag)
        }
    }
}