package org.sorz.lab.tinykeepass.autofill

import android.content.Context
import android.os.Build
import android.service.autofill.Dataset
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import android.view.autofill.AutofillValue
import android.widget.RemoteViews

import org.sorz.lab.tinykeepass.R

import java.util.stream.Stream

import de.slackspace.openkeepass.domain.Entry
import org.sorz.lab.tinykeepass.keepass.icon


@RequiresApi(api = Build.VERSION_CODES.O)
internal object AutofillUtils {

    fun getRemoteViews(context: Context, text: String, @DrawableRes icon: Int): RemoteViews {
        return RemoteViews(context.packageName, R.layout.autofill_service_list_item).apply {
            setTextViewText(R.id.textView, text)
            setImageViewResource(R.id.imageIcon, icon)
        }
    }

    fun buildDataset(context: Context, entry: Entry,
                     struct: StructureParser.Result): Dataset? {
        val title = makeEntryTitle(context, entry)
        val views = getRemoteViews(context, title, R.drawable.ic_person_blue_24dp).apply {
            setImageViewBitmap(R.id.imageIcon, entry.icon)
        }
        val builder = Dataset.Builder(views).apply {
            setId(entry.uuid.toString())
        }

        if (entry.password.isNotBlank()) {
            val value = AutofillValue.forText(entry.password)
            struct.password.forEach { id -> builder.setValue(id, value) }
        }
        if (entry.username.isNotBlank()) {
            val value = AutofillValue.forText(entry.username)
            var ids = struct.username.stream()
            if (entry.username.contains("@") || struct.username.isEmpty())
                ids = Stream.concat(ids, struct.email.stream())
            ids.forEach { id -> builder.setValue(id, value) }
        }
        return try {
            builder.build()
        } catch (e: IllegalArgumentException) {
            // if not value be set
            null
        }
    }

    private fun makeEntryTitle(context: Context, entry: Entry): String = entry.run {
        when {
            title.isNotBlank() && username.isNotBlank() -> "$title ($username)"
            title.isNotBlank() -> title
            username.isNotBlank() -> username
            notes.isNotBlank() -> notes.trim()
            else -> context.getString(R.string.autofill_not_title)
        }
    }
}
