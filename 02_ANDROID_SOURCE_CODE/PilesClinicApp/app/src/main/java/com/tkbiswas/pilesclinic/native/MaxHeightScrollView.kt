package com.tkbiswas.pilesclinic.native

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ScrollView

/**
 * TK-REQUESTED ADDITION (2026-07-20): fixes the same "popup form doesn't
 * scroll" class of bug (see 00_PROJECT_STATE_MASTER_NOTE.md section 88) in
 * the few popups whose Save/Cancel buttons are custom -- baked directly into
 * the same root view passed to AlertDialog, rather than AlertDialog's own
 * button bar. A plain ScrollView with no height cap wouldn't actually
 * enforce scrolling there (it would just grow as tall as its content, same
 * as before, defeating the fix), and a ScrollView with a FIXED height would
 * leave ugly empty space under short content -- a real design change TK
 * does not want.
 *
 * This ScrollView only scrolls once its content is actually taller than
 * [maxHeightPx]; shorter content (the normal case) is shown at its natural
 * height with no visual difference from before.
 */
class MaxHeightScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ScrollView(context, attrs) {

    var maxHeightPx: Int = Int.MAX_VALUE

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val cappedSpec = if (maxHeightPx < Int.MAX_VALUE) {
            View.MeasureSpec.makeMeasureSpec(maxHeightPx, View.MeasureSpec.AT_MOST)
        } else {
            heightMeasureSpec
        }
        super.onMeasure(widthMeasureSpec, cappedSpec)
    }
}
