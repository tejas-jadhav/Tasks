package com.example.tasks.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AccelerateInterpolator
import androidx.appcompat.view.ActionMode
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.tasks.R
import com.example.tasks.TasksApplication
import com.example.tasks.data.db.TasksDatabase
import com.example.tasks.data.repository.TasksRepository
import com.example.tasks.databinding.ActivityMainBinding
import com.example.tasks.utils.Constants
import com.example.tasks.viewmodel.TasksViewModel
import com.example.tasks.viewmodel.TasksViewModelFactory

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"


//    todo viewAs
//    todo notification
//    todo calendar
//    todo settings

    private lateinit var _binding: ActivityMainBinding
    private val binding: ActivityMainBinding get() = _binding
    private var _navController: NavController? = null
    val navController: NavController get() = _navController!!
    private var overflowMenu: Menu? = null
    private var _backCallBack: ((NavController) -> Unit)? = null
    private var _handleMenuOptions: ((MenuItem) -> Unit)? = null

    private var _tasksViewModel: TasksViewModel? = null
    val tasksViewModel: TasksViewModel get() = _tasksViewModel!!
    var _isActionModeEnabled = false
    val isActionModeEnabled get() = _isActionModeEnabled
    private var _actionDeleteCallback: (() -> Unit)? = null
    private var _onDestroyActionMode: (() -> Unit)? = null
    var actionMode: ActionMode? = null

    fun getMainView(): View {
        return binding.root
    }


    //    Activity Lifecycle code
    override fun onCreate(savedInstanceState: Bundle?) {
//        imp handle splashscreen before onCreate
        initializeTasksViewModel()
        installSplashScreen().apply {
            setKeepOnScreenCondition {
                tasksViewModel.initialLoading.value
            }
        }
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(_binding.root)
        setUpBottomNavigation()
    }

    override fun onResume() {
        super.onResume()
        navController.addOnDestinationChangedListener(navChangeListener)
    }

    override fun onPause() {
        navController.removeOnDestinationChangedListener(navChangeListener)
        super.onPause()
    }


    //    init stuff
    private fun initializeTasksViewModel() {
        val repository = TasksRepository(TasksDatabase.getDatabase(this))
        val tasksViewModelFactory = TasksViewModelFactory(repository, application)
        _tasksViewModel = ViewModelProvider(this, tasksViewModelFactory)[TasksViewModel::class.java]
    }

    private fun setUpBottomNavigation() {
        _navController =
            (binding.fragmentContainerView.getFragment() as NavHostFragment).navController
        binding.bottomNavigationView.setupWithNavController(navController)
    }

    private val navChangeListener =
        NavController.OnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.taskFragment -> onTaskPage()
                R.id.taskListFragment -> onListPage()
                R.id.calendarFragment -> onCalendarPage()
                R.id.settingsFragment -> onSettingsPage()
                R.id.addTaskFragment -> onAddTaskPage()
                R.id.individualTaskFragment -> onIndividualTaskPage()
                R.id.editTaskFragment -> onEditTaskPage()
                R.id.completedTaskFragment -> onCompletedTaskPage()
                R.id.viewTaskListFragment -> onViewTaskListFragment()
            }
        }


    // menu stuff
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        var menuToInflate = R.menu.task_menu
        navController.currentDestination?.let { currentFragment ->
            menuToInflate = when (currentFragment.id) {
                R.id.calendarFragment -> return false
                R.id.settingsFragment -> return false
                R.id.taskListFragment -> R.menu.task_list_menu
                R.id.taskFragment -> R.menu.task_menu
                R.id.addTaskFragment -> R.menu.accept_menu
                R.id.individualTaskFragment -> R.menu.individual_task_menu
                R.id.editTaskFragment -> R.menu.accept_menu
                R.id.completedTaskFragment -> R.menu.action_menu
                R.id.viewTaskListFragment -> R.menu.add_menu
                else -> return false
            }
        }
        menuInflater.inflate(menuToInflate, menu)
        overflowMenu = menu

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        _handleMenuOptions?.let {
            it(item)
        }
        return false
    }

    fun setHandleMenuOptions(handler: (MenuItem) -> Unit) {
        _handleMenuOptions = handler
    }

    private fun clearMenuClickHandler() {
        _handleMenuOptions = null
    }

//    fragment stuff

    private fun onTaskPage() {
        setActionBarTitle("All Tasks")
        hideActionBarBackButton()
        resetBackCallback()
        showBottomNavigationView()
        overflowMenu?.let {
            it.clear()
            menuInflater.inflate(R.menu.task_menu, it)
        }
    }

    private fun onListPage() {
        setActionBarTitle("My Lists")
        hideActionBarBackButton()
        resetBackCallback()
        showBottomNavigationView()
//        clearMenuClickHandler()
        overflowMenu?.let {
            it.clear()
            menuInflater.inflate(R.menu.task_list_menu, it)
        }
    }

    private fun onCalendarPage() {
        setActionBarTitle("Calendar")
        hideActionBarBackButton()
        resetBackCallback()
        clearMenuClickHandler()
        showBottomNavigationView()
        overflowMenu?.clear()
    }

    private fun onSettingsPage() {
        setActionBarTitle("Settings")
        resetBackCallback()
        showActionBarBackButton()
        hideBottomNavigationView()
        clearMenuClickHandler()
        overflowMenu?.clear()
    }

    private fun onAddTaskPage() {
        setActionBarTitle("Add New Task")
        resetBackCallback()
        clearMenuClickHandler()
        overflowMenu?.clear()
        overflowMenu?.let {
            menuInflater.inflate(R.menu.accept_menu, it)
        }
        showActionBarBackButton()
        hideBottomNavigationView()

    }

    private fun onIndividualTaskPage() {
        setActionBarTitle("")
        resetBackCallback()
        clearMenuClickHandler()
        overflowMenu?.clear()
        overflowMenu?.let {
            it.clear()
            menuInflater.inflate(R.menu.individual_task_menu, it)
        }
        showActionBarBackButton()
        hideBottomNavigationView()
    }

    private fun onEditTaskPage() {
        setActionBarTitle("Edit Task")
        resetBackCallback()
        clearMenuClickHandler()
        overflowMenu?.clear()
        overflowMenu?.let {
            it.clear()
            menuInflater.inflate(R.menu.accept_menu, it)
        }
        showActionBarBackButton()
    }

    private fun onCompletedTaskPage() {
        setActionBarTitle("Completed Tasks")
        resetBackCallback()
        clearMenuClickHandler()
        overflowMenu?.clear()
        overflowMenu?.let {
            it.clear()
            menuInflater.inflate(R.menu.action_menu, it)
        }
        hideBottomNavigationView()
        showActionBarBackButton()
    }

    private fun onViewTaskListFragment() {
        resetBackCallback()
        clearMenuClickHandler()
        overflowMenu?.clear()
        overflowMenu?.let {
            it.clear()
            menuInflater.inflate(R.menu.add_menu, it)
        }
        hideBottomNavigationView()
        showActionBarBackButton()
    }



    //    action bar stuff
    fun setActionBarTitle(title: String) {
        supportActionBar?.title = title
    }

    private fun showActionBarBackButton() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun hideActionBarBackButton() {
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }

    //    bottom nav stuff
    fun showBottomNavigationView() {
        val bottomNavView = binding.bottomNavigationView
        if (!bottomNavView.isVisible) {
            bottomNavView.isVisible = true
            bottomNavView.animate().apply {
                translationY(0.0F)
                duration = Constants.BOTTOM_NAVIGATION_ANIMATION_DURATION
                interpolator = AccelerateInterpolator()
                setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(p0: Animator?) {
                        bottomNavView.isVisible = true
                    }
                })
                start()
            }
        }
    }

    fun hideBottomNavigationView() {
        val bottomNavView = binding.bottomNavigationView
        if (bottomNavView.isVisible) {
            bottomNavView.animate().apply {
                translationY(bottomNavView.height.toFloat())
                duration = Constants.BOTTOM_NAVIGATION_ANIMATION_DURATION
                interpolator = AccelerateInterpolator()
                setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(p0: Animator?) {
                        bottomNavView.isVisible = false
                    }
                })
                start()
            }
            bottomNavView.isVisible = false
        }
    }


    //    action mode stuff
    fun setActionDeleteCallback(callback: () -> Unit) {
        _actionDeleteCallback = callback
    }

    fun setOnDestroyActionMode(callback: () -> Unit) {
        _onDestroyActionMode = callback
    }

    fun openSupportActionMode(actionModeCallback: ActionMode.Callback) {
        actionMode = startSupportActionMode(actionModeCallback)
    }

    fun closeSupportActionMode() {
        actionMode?.finish()
    }

    fun setSupportActionModeTitle(text: String) {
        if (isActionModeEnabled && actionMode != null) {
            actionMode?.title = text
        }
    }


    //    back stuff
    fun setBackCallback(callback: (NavController) -> Unit) {
        _backCallBack = callback
    }

    private fun resetBackCallback() {
        _backCallBack = null
    }

    override fun onSupportNavigateUp(): Boolean {
        _backCallBack?.let {
            it(navController)
        } ?: navController.popBackStack()

        return true
    }

    override fun onBackPressed() {
        if (navController.backQueue.size > 2) {
            _backCallBack?.let {
                it(navController)
            } ?: navController.popBackStack()
        } else {
            super.onBackPressed()
        }
    }
}