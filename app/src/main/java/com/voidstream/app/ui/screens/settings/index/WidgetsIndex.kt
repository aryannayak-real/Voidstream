package com.voidstream.app.ui.screens.settings.index

import com.voidstream.app.R
import com.voidstream.app.ui.components.settings.SettingEntry
import com.voidstream.app.ui.components.settings.SettingsDestination
import com.voidstream.app.widget.core.FlowWidgetEntry
import com.voidstream.app.widget.core.FlowWidgets

internal object WidgetsIndex {
    fun entryFor(widget: FlowWidgetEntry) =
        SettingEntry(
            key = "widgets.${widget.id}",
            title = widget.label,
            summary = widget.description,
            section = R.string.settings_widgets_add_header,
            keywords = R.string.settings_keywords_widgets,
            destination = SettingsDestination.WIDGETS,
        )

    val all = FlowWidgets.catalog.map(::entryFor)
}
