package com.woocommerce.android.ui.inbox

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.woocommerce.android.R
import com.woocommerce.android.compose.utils.toAnnotatedString
import com.woocommerce.android.ui.compose.animations.skeletonAnimationBrush
import com.woocommerce.android.ui.inbox.InboxViewModel.InboxNoteActionUi
import com.woocommerce.android.ui.inbox.InboxViewModel.InboxNoteUi
import com.woocommerce.android.ui.inbox.InboxViewModel.InboxState
import com.woocommerce.android.util.StringUtils

@Composable
fun Inbox(viewModel: InboxViewModel) {
    val inboxState by viewModel.inboxState.observeAsState(InboxState())
    Inbox(state = inboxState)
}

@Composable
fun Inbox(state: InboxState) {
    when {
        state.isLoading -> InboxSkeleton()
        else -> InboxNotes(
            notes = state.notes,
            onRefresh = state.onRefresh,
            isRefreshing = state.isRefreshing
        )
    }
}

@Composable
fun InboxEmptyCase() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.empty_inbox_title),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp)
        )
        Spacer(Modifier.size(54.dp))
        Image(
            painter = painterResource(id = R.drawable.img_empty_inbox),
            contentDescription = null,
        )
        Spacer(Modifier.size(48.dp))
        Text(
            text = stringResource(id = R.string.empty_inbox_description),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.body1,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp)
        )
    }
}

@Composable
fun InboxNotes(
    notes: List<InboxNoteUi>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing),
        onRefresh = { onRefresh.invoke() },
        indicator = { state, trigger ->
            SwipeRefreshIndicator(
                state = state,
                refreshTriggerDistance = trigger,
                contentColor = MaterialTheme.colors.primary,
            )
        }
    ) {
        if (notes.isEmpty()) {
            InboxEmptyCase()
        } else {
            LazyColumn {
                itemsIndexed(notes) { index, note ->
                    InboxNoteRow(note = note)
                    if (index < notes.lastIndex)
                        Divider(
                            color = colorResource(id = R.color.divider_color),
                            thickness = 1.dp
                        )
                }
            }
        }
    }
}

@Composable
fun InboxNoteRow(note: InboxNoteUi) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                modifier = Modifier.padding(top = 16.dp),
                text = note.dateCreated,
                style = MaterialTheme.typography.subtitle2,
                color = colorResource(id = R.color.color_surface_variant)
            )
            Text(
                text = note.title,
                fontWeight = if (note.isActioned) FontWeight.Normal else FontWeight.Bold,
                style = MaterialTheme.typography.subtitle1
            )
            Text(
                text = StringUtils.fromHtml(note.description).toAnnotatedString(),
                style = MaterialTheme.typography.body2
            )
        }
        when {
            note.isSurvey -> InboxNoteSurveyActionsRow(note.actions)
            else -> InboxNoteActionsRow(note.actions)
        }
    }
}

@Composable
private fun InboxNoteActionsRow(actions: List<InboxNoteActionUi>) {
    LazyRow(
        Modifier.padding(start = 8.dp, end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(actions) { action ->
            InboxNoteTextAction(inboxAction = action)
        }
    }
}

@Composable
private fun InboxNoteSurveyActionsRow(actions: List<InboxNoteActionUi>) {
    Row(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (actions.isEmpty()) {
            Text(
                modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
                text = stringResource(id = R.string.inbox_note_survey_actioned),
                style = MaterialTheme.typography.body2
            )
        } else {
            actions.forEachIndexed { index, inboxNoteActionUi ->
                when {
                    index < 2 -> InboxNoteSurveyAction(inboxNoteActionUi)
                    else -> InboxNoteTextAction(inboxNoteActionUi)
                }
            }
        }
    }
}

@Composable
fun InboxNoteTextAction(inboxAction: InboxNoteActionUi) {
    TextButton(onClick = { inboxAction.onClick(inboxAction.id, inboxAction.parentNoteId) }) {
        Text(
            text = inboxAction.label.uppercase(),
            color = colorResource(id = inboxAction.textColor)
        )
    }
}

@Composable
@SuppressWarnings("MagicNumber")
fun InboxNoteSurveyAction(inboxAction: InboxNoteActionUi) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(
            onClick = { inboxAction.onClick(inboxAction.id, inboxAction.parentNoteId) },
            border = BorderStroke(1.dp, colorResource(id = R.color.color_on_surface_disabled)),
            shape = RoundedCornerShape(20),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.background,
            )
        ) {
            Text(
                text = inboxAction.label,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.subtitle1
            )
        }
    }
}

@Composable
@Suppress("MagicNumber")
fun InboxSkeleton() {
    val numberOfInboxSkeletonRows = 4
    LazyColumn {
        repeat(numberOfInboxSkeletonRows) {
            item {
                InboxNoteItemSkeleton(brush = skeletonAnimationBrush())
                Divider(
                    color = colorResource(id = R.color.divider_color),
                    thickness = 1.dp
                )
            }
        }
    }
}

@Composable
fun InboxNoteItemSkeleton(
    brush: Brush
) {
    Column(modifier = Modifier.padding(16.dp)) {
        InboxNoteHeaderSkeleton(brush)
        Spacer(modifier = Modifier.padding(top = 16.dp))
        InboxNoteContentRowsSkeleton(brush)
        Spacer(modifier = Modifier.padding(top = 14.dp))
        InboxNoteButtonsSkeleton(brush)
    }
}

@Composable
private fun InboxNoteHeaderSkeleton(brush: Brush) {
    Spacer(
        modifier = Modifier
            .width(96.dp)
            .height(16.dp)
            .background(brush = brush)
    )
    Spacer(modifier = Modifier.padding(top = 20.dp))
    Spacer(
        modifier = Modifier
            .width(190.dp)
            .height(16.dp)
            .background(brush = brush)
    )
}

@Composable
private fun InboxNoteContentRowsSkeleton(brush: Brush) {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(16.dp)
            .background(brush = brush)
    )
    Spacer(modifier = Modifier.padding(top = 6.dp))
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(16.dp)
            .background(brush = brush)
    )
    Spacer(modifier = Modifier.padding(top = 6.dp))
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(16.dp)
            .background(brush = brush)
    )
}

@Composable
private fun InboxNoteButtonsSkeleton(brush: Brush) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(
            modifier = Modifier
                .width(150.dp)
                .height(16.dp)
                .background(brush = brush)
        )
        Spacer(
            modifier = Modifier
                .width(60.dp)
                .height(16.dp)
                .background(brush = brush)
        )
    }
}

@Preview
@Composable
fun InboxPreview(@PreviewParameter(SampleInboxProvider::class, 1) state: InboxState) {
    Inbox(state)
}

class SampleInboxProvider : PreviewParameterProvider<InboxState> {
    override val values = sequenceOf(
        InboxState(
            isLoading = false,
            notes = listOf(
                InboxNoteUi(
                    id = 1,
                    title = "Install the Facebook free extension",
                    description = "description",
                    dateCreated = "5h ago",
                    actions = listOf(
                        InboxNoteActionUi(
                            id = 3,
                            parentNoteId = 1,
                            label = "Open",
                            textColor = R.color.color_secondary,
                            onClick = { _, _ -> },
                            url = "",
                        ),
                        InboxNoteActionUi(
                            id = 4,
                            parentNoteId = 1,
                            label = "Dismiss",
                            textColor = R.color.color_surface_variant,
                            onClick = { _, _ -> },
                            url = "",
                        )
                    ),
                    isActioned = false,
                    isSurvey = false
                ),
                InboxNoteUi(
                    id = 2,
                    title = "Connect with your audience",
                    description = "Description",
                    dateCreated = "22 minutes ago",
                    actions = listOf(
                        InboxNoteActionUi(
                            id = 3,
                            parentNoteId = 2,
                            label = "Open",
                            textColor = R.color.color_secondary,
                            onClick = { _, _ -> },
                            url = "",
                        ),
                        InboxNoteActionUi(
                            id = 4,
                            parentNoteId = 2,
                            label = "Dismiss",
                            textColor = R.color.color_surface_variant,
                            onClick = { _, _ -> },
                            url = "",
                        )
                    ),
                    isActioned = false,
                    isSurvey = true
                )
            )
        )
    )
}
