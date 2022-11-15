package com.udacity.project4.locationreminders

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityReminderDescriptionBinding
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem

/**
 * Activity that displays the reminder details after the user clicks on the notification
 */
class ReminderDescriptionActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_ReminderDataItem = "EXTRA_ReminderDataItem"

        /** One common use case for reified type parameters is building adapters for APIs that take parameters of type java.lang.Class. Instead of passing the class of the activity as a Class, a reified type parameter is used. The ::class.java syntax shows how you can get a java.lang.Class corresponding to a Kotlin class. Specifying a class as a type argument is easier to read because it’s shorter than the ::class.java syntax you need to use otherwise.

        // old way
        fun newIntent(context: Context, reminderDataItem: ReminderDataItem): Intent {
        val intent = Intent(context, ReminderDescriptionActivity::class.java)
        intent.putExtra(EXTRA_ReminderDataItem, reminderDataItem)
        return intent
        }


        // better way
        inline fun <reified T : Activity> Context.createIntent(vararg args: Pair<String, Any>) : Intent {
        val intent = Intent(this, T::class.java)
        intent.putExtras(bundleOf(*args))
        return intent
        }


        // usage
        fun newIntent(context: Context, reminderDataItem: ReminderDataItem): Intent {
        return context.createIntent<ReminderDescriptionActivity>(EXTRA_ReminderDataItem to reminderDataItem)
        }
        We also defined an explicit reified type parameter bound to make sure that we can only use Activity (and its subclasses) as the type argument. **/

        // Receive the reminder object after the user clicks on the notification
        fun newIntent(context: Context, reminderDataItem: ReminderDataItem): Intent {
            val intent = Intent(context, ReminderDescriptionActivity::class.java)
            intent.putExtra(EXTRA_ReminderDataItem, reminderDataItem)
            return intent
        }
    }

    private lateinit var binding: ActivityReminderDescriptionBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_reminder_description)

        // TODO Done: Add the implementation of the reminder details
        // Get the needed data from intent
        // val data : ReminderDataItem = intent.getSerializableExtra(EXTRA_ReminderDataItem) as ReminderDataItem
        val data : ReminderDataItem = intent.extras?.getSerializable(EXTRA_ReminderDataItem) as ReminderDataItem
        binding.reminderDataItem = data     // Bind an object of model class to the layout
    }
}
