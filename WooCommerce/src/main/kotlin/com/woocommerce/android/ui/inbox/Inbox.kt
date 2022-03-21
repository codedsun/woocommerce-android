package com.woocommerce.android.ui.inbox

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.animations.skeletonAnimationBrush
import com.woocommerce.android.ui.inbox.InboxViewModel.InboxNoteUi
import com.woocommerce.android.ui.inbox.InboxViewModel.InboxState

@Composable
fun Inbox(viewModel: InboxViewModel) {
    val inboxState by viewModel.inboxState.observeAsState(InboxState())
    Inbox(state = inboxState)
}

@Composable
fun Inbox(state: InboxState) {
    when {
        state.isLoading -> InboxSkeleton()
        state.notes.isEmpty() -> InboxEmptyCase()
        state.notes.isNotEmpty() -> InboxNotes(notes = state.notes)
    }
}

@Composable
fun InboxEmptyCase() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
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
fun InboxNotes(notes: List<InboxNoteUi>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
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

@Composable
fun InboxNoteRow(note: InboxNoteUi) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = note.updatedTime,
            style = MaterialTheme.typography.subtitle2,
            color = colorResource(id = R.color.color_surface_variant)
        )
        Text(
            text = note.title,
            style = MaterialTheme.typography.subtitle1
        )
        Text(
            text = note.description,
            style = MaterialTheme.typography.body2
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            TextButton(
                onClick = { note.onCallToActionClick(note.id) }
            ) {
                Text(
                    text = note.callToActionText.uppercase(),
                    color = MaterialTheme.colors.secondary
                )
            }

            TextButton(
                onClick = { note.onDismissNote(note.id) },
                Modifier.padding(start = 16.dp)
            ) {
                Text(
                    text = note.dismissText.uppercase(),
                    color = colorResource(id = R.color.color_surface_variant)
                )
            }
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
                    id = "1",
                    title = "Install the Facebook free extension",
                    description = "Now that your store is set up, you’re ready to begin marketing it. " +
                        "Head over to the WooCommerce marketing panel to get started.",
                    updatedTime = "5h ago",
                    callToActionText = "Learn more",
                    onCallToActionClick = {},
                    dismissText = "Dismiss",
                    onDismissNote = {},
                    isRead = false
                ),
                InboxNoteUi(
                    id = "2",
                    title = "Connect with your audience",
                    description = "Grow your customer base and increase your sales with marketing tools " +
                        "built for WooCommerce.",
                    updatedTime = "22 minutes ago",
                    callToActionText = "Learn more",
                    onCallToActionClick = {},
                    dismissText = "Dismiss",
                    onDismissNote = {},
                    isRead = false
                )
            )
        )
    )
}
