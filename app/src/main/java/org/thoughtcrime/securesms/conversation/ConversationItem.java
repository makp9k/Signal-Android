/*
 * Copyright (C) 2011 Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.thoughtcrime.securesms.conversation;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.Annotation;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.text.style.CharacterStyle;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.text.util.LinkifyCompat;
import androidx.lifecycle.LifecycleOwner;

import com.annimon.stream.Stream;
import com.google.android.exoplayer2.MediaItem;
import com.google.common.collect.Sets;

import org.signal.core.util.logging.Log;
import org.thoughtcrime.securesms.BindableConversationItem;
import org.thoughtcrime.securesms.MediaPreviewActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.attachments.DatabaseAttachment;
import org.thoughtcrime.securesms.components.AlertView;
import org.thoughtcrime.securesms.components.AudioView;
import org.thoughtcrime.securesms.components.AvatarImageView;
import org.thoughtcrime.securesms.components.BorderlessImageView;
import org.thoughtcrime.securesms.components.ConversationItemFooter;
import org.thoughtcrime.securesms.components.ConversationItemThumbnail;
import org.thoughtcrime.securesms.components.DocumentView;
import org.thoughtcrime.securesms.components.LinkPreviewView;
import org.thoughtcrime.securesms.components.Outliner;
import org.thoughtcrime.securesms.components.PlaybackSpeedToggleTextView;
import org.thoughtcrime.securesms.components.QuoteView;
import org.thoughtcrime.securesms.components.SharedContactView;
import org.thoughtcrime.securesms.components.emoji.EmojiTextView;
import org.thoughtcrime.securesms.components.mention.MentionAnnotation;
import org.thoughtcrime.securesms.contactshare.Contact;
import org.thoughtcrime.securesms.conversation.colors.ChatColors;
import org.thoughtcrime.securesms.conversation.colors.Colorizer;
import org.thoughtcrime.securesms.conversation.mutiselect.MultiselectCollection;
import org.thoughtcrime.securesms.conversation.mutiselect.MultiselectPart;
import org.thoughtcrime.securesms.database.AttachmentDatabase;
import org.thoughtcrime.securesms.database.DatabaseFactory;
import org.thoughtcrime.securesms.database.MessageDatabase;
import org.thoughtcrime.securesms.database.model.MediaMmsMessageRecord;
import org.thoughtcrime.securesms.database.model.MessageRecord;
import org.thoughtcrime.securesms.database.model.MmsMessageRecord;
import org.thoughtcrime.securesms.database.model.Quote;
import org.thoughtcrime.securesms.dependencies.ApplicationDependencies;
import org.thoughtcrime.securesms.giph.mp4.GiphyMp4PlaybackPolicy;
import org.thoughtcrime.securesms.giph.mp4.GiphyMp4PlaybackPolicyEnforcer;
import org.thoughtcrime.securesms.jobs.AttachmentDownloadJob;
import org.thoughtcrime.securesms.jobs.MmsDownloadJob;
import org.thoughtcrime.securesms.jobs.MmsSendJob;
import org.thoughtcrime.securesms.jobs.SmsSendJob;
import org.thoughtcrime.securesms.keyvalue.SignalStore;
import org.thoughtcrime.securesms.linkpreview.LinkPreview;
import org.thoughtcrime.securesms.linkpreview.LinkPreviewUtil;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.mms.ImageSlide;
import org.thoughtcrime.securesms.mms.PartAuthority;
import org.thoughtcrime.securesms.mms.Slide;
import org.thoughtcrime.securesms.mms.SlideClickListener;
import org.thoughtcrime.securesms.mms.SlidesClickedListener;
import org.thoughtcrime.securesms.mms.TextSlide;
import org.thoughtcrime.securesms.mms.VideoSlide;
import org.thoughtcrime.securesms.reactions.ReactionsConversationView;
import org.thoughtcrime.securesms.recipients.LiveRecipient;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.recipients.RecipientForeverObserver;
import org.thoughtcrime.securesms.recipients.RecipientId;
import org.thoughtcrime.securesms.revealable.ViewOnceMessageView;
import org.thoughtcrime.securesms.util.DateUtils;
import org.thoughtcrime.securesms.util.InterceptableLongClickCopyLinkSpan;
import org.thoughtcrime.securesms.util.LongClickMovementMethod;
import org.thoughtcrime.securesms.util.MessageRecordUtil;
import org.thoughtcrime.securesms.util.Projection;
import org.thoughtcrime.securesms.util.SearchUtil;
import org.thoughtcrime.securesms.util.StringUtil;
import org.thoughtcrime.securesms.util.ThemeUtil;
import org.thoughtcrime.securesms.util.UrlClickHandler;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.VibrateUtil;
import org.thoughtcrime.securesms.util.ViewUtil;
import org.thoughtcrime.securesms.util.views.NullableStub;
import org.thoughtcrime.securesms.util.views.Stub;
import org.whispersystems.libsignal.util.guava.Optional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * A view that displays an individual conversation item within a conversation
 * thread.  Used by ComposeMessageActivity's ListActivity via a ConversationAdapter.
 *
 * @author Moxie Marlinspike
 *
 */

public final class ConversationItem extends RelativeLayout implements BindableConversationItem,
    RecipientForeverObserver
{
  private static final String TAG = Log.tag(ConversationItem.class);

  private static final int MAX_MEASURE_CALLS       = 3;

  private static final Rect SWIPE_RECT = new Rect();

  private ConversationMessage     conversationMessage;
  private MessageRecord           messageRecord;
  private Optional<MessageRecord> nextMessageRecord;
  private Locale                  locale;
  private boolean                 groupThread;
  private LiveRecipient           recipient;
  private GlideRequests           glideRequests;
  private ValueAnimator           pulseOutlinerAlphaAnimator;

            protected ConversationItemBodyBubble bodyBubble;
            protected View                       reply;
            protected View                       replyIcon;
  @Nullable protected ViewGroup                  contactPhotoHolder;
  @Nullable private   QuoteView                  quoteView;
            private   EmojiTextView              bodyText;
            private   ConversationItemFooter     footer;
  @Nullable private   ConversationItemFooter     stickerFooter;
  @Nullable private   TextView                   groupSender;
  @Nullable private   View                       groupSenderHolder;
            private   AvatarImageView            contactPhoto;
            private   AlertView                  alertView;
            protected ReactionsConversationView  reactionsView;

  private @NonNull  Set<MultiselectPart>                    batchSelected = new HashSet<>();
  private @NonNull  Outliner                                outliner      = new Outliner();
  private @NonNull  Outliner                                pulseOutliner = new Outliner();
  private @NonNull  List<Outliner>                          outliners     = new ArrayList<>(2);
  private           LiveRecipient                           conversationRecipient;
  private           NullableStub<ConversationItemThumbnail> mediaThumbnailStub;
  private           Stub<AudioView>                         audioViewStub;
  private           Stub<DocumentView>                      documentViewStub;
  private           Stub<SharedContactView>                 sharedContactStub;
  private           Stub<LinkPreviewView>                   linkPreviewStub;
  private           Stub<BorderlessImageView>               stickerStub;
  private           Stub<ViewOnceMessageView>               revealableStub;
  private @Nullable EventListener                           eventListener;

  private int     defaultBubbleColor;
  private int     defaultBubbleColorForWallpaper;
  private int     measureCalls;
  private boolean updatingFooter;

  private final PassthroughClickListener        passthroughClickListener     = new PassthroughClickListener();
  private final AttachmentDownloadClickListener downloadClickListener        = new AttachmentDownloadClickListener();
  private final SlideClickPassthroughListener   singleDownloadClickListener  = new SlideClickPassthroughListener(downloadClickListener);
  private final SharedContactEventListener      sharedContactEventListener   = new SharedContactEventListener();
  private final SharedContactClickListener      sharedContactClickListener   = new SharedContactClickListener();
  private final LinkPreviewClickListener        linkPreviewClickListener     = new LinkPreviewClickListener();
  private final ViewOnceMessageClickListener    revealableClickListener      = new ViewOnceMessageClickListener();
  private final UrlClickListener                urlClickListener             = new UrlClickListener();
  private final Rect                            thumbnailMaskingRect         = new Rect();
  private final TouchDelegateChangedListener    touchDelegateChangedListener = new TouchDelegateChangedListener();

  private final Context context;

  private MediaItem          mediaItem;
  private boolean            canPlayContent;
  private Projection.Corners bodyBubbleCorners;
  private Colorizer          colorizer;
  private boolean            hasWallpaper;
  private float              lastYDownRelativeToThis;

  public ConversationItem(Context context) {
    this(context, null);
  }

  public ConversationItem(Context context, AttributeSet attrs) {
    super(context, attrs);
    this.context = context;
  }

  @Override
  public void setOnClickListener(OnClickListener l) {
    super.setOnClickListener(new ClickListener(l));
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();

    initializeAttributes();

    this.bodyText                =                    findViewById(R.id.conversation_item_body);
    this.footer                  =                    findViewById(R.id.conversation_item_footer);
    this.stickerFooter           =                    findViewById(R.id.conversation_item_sticker_footer);
    this.groupSender             =                    findViewById(R.id.group_message_sender);
    this.alertView               =                    findViewById(R.id.indicators_parent);
    this.contactPhoto            =                    findViewById(R.id.contact_photo);
    this.contactPhotoHolder      =                    findViewById(R.id.contact_photo_container);
    this.bodyBubble              =                    findViewById(R.id.body_bubble);
    this.mediaThumbnailStub      = new NullableStub<>(findViewById(R.id.image_view_stub));
    this.audioViewStub           =         new Stub<>(findViewById(R.id.audio_view_stub));
    this.documentViewStub        =         new Stub<>(findViewById(R.id.document_view_stub));
    this.sharedContactStub       =         new Stub<>(findViewById(R.id.shared_contact_view_stub));
    this.linkPreviewStub         =         new Stub<>(findViewById(R.id.link_preview_stub));
    this.stickerStub             =         new Stub<>(findViewById(R.id.sticker_view_stub));
    this.revealableStub          =         new Stub<>(findViewById(R.id.revealable_view_stub));
    this.groupSenderHolder       =                    findViewById(R.id.group_sender_holder);
    this.quoteView               =                    findViewById(R.id.quote_view);
    this.reply                   =                    findViewById(R.id.reply_icon_wrapper);
    this.replyIcon               =                    findViewById(R.id.reply_icon);
    this.reactionsView           =                    findViewById(R.id.reactions_view);

    setOnClickListener(new ClickListener(null));

    bodyText.setOnLongClickListener(passthroughClickListener);
    bodyText.setOnClickListener(passthroughClickListener);
    footer.setOnTouchDelegateChangedListener(touchDelegateChangedListener);
  }

  @Override
  public void bind(@NonNull LifecycleOwner lifecycleOwner,
                   @NonNull ConversationMessage conversationMessage,
                   @NonNull Optional<MessageRecord> previousMessageRecord,
                   @NonNull Optional<MessageRecord> nextMessageRecord,
                   @NonNull GlideRequests glideRequests,
                   @NonNull Locale locale,
                   @NonNull Set<MultiselectPart> batchSelected,
                   @NonNull Recipient conversationRecipient,
                   @Nullable String searchQuery,
                   boolean pulse,
                   boolean hasWallpaper,
                   boolean isMessageRequestAccepted,
                   boolean allowedToPlayInline,
                   @NonNull Colorizer colorizer)
  {
    if (this.recipient != null) this.recipient.removeForeverObserver(this);
    if (this.conversationRecipient != null) this.conversationRecipient.removeForeverObserver(this);

    lastYDownRelativeToThis = 0;

    conversationRecipient = conversationRecipient.resolve();

    this.conversationMessage    = conversationMessage;
    this.messageRecord          = conversationMessage.getMessageRecord();
    this.nextMessageRecord      = nextMessageRecord;
    this.locale                 = locale;
    this.glideRequests          = glideRequests;
    this.batchSelected          = batchSelected;
    this.conversationRecipient  = conversationRecipient.live();
    this.groupThread            = conversationRecipient.isGroup();
    this.recipient              = messageRecord.getIndividualRecipient().live();
    this.canPlayContent         = false;
    this.mediaItem              = null;
    this.colorizer              = colorizer;

    this.recipient.observeForever(this);
    this.conversationRecipient.observeForever(this);

    setGutterSizes(messageRecord, groupThread);
    setMessageShape(messageRecord, previousMessageRecord, nextMessageRecord, groupThread);
    setMediaAttributes(messageRecord, previousMessageRecord, nextMessageRecord, groupThread, hasWallpaper, isMessageRequestAccepted, allowedToPlayInline);
    setBodyText(messageRecord, searchQuery, isMessageRequestAccepted);
    setBubbleState(messageRecord, messageRecord.getRecipient(), hasWallpaper, colorizer);
    setInteractionState(conversationMessage, pulse);
    setStatusIcons(messageRecord, hasWallpaper);
    setContactPhoto(recipient.get());
    setGroupMessageStatus(messageRecord, recipient.get());
    setGroupAuthorColor(messageRecord, hasWallpaper, colorizer);
    setAuthor(messageRecord, previousMessageRecord, nextMessageRecord, groupThread, hasWallpaper);
    setQuote(messageRecord, previousMessageRecord, nextMessageRecord, groupThread, messageRecord.getRecipient().getChatColors());
    setMessageSpacing(context, messageRecord, previousMessageRecord, nextMessageRecord, groupThread);
    setReactions(messageRecord);
    setFooter(messageRecord, nextMessageRecord, locale, groupThread, hasWallpaper);
  }

  @Override
  public void updateTimestamps() {
    getActiveFooter(messageRecord).setMessageRecord(messageRecord, locale);
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    if (ev.getAction() == MotionEvent.ACTION_DOWN) {
      lastYDownRelativeToThis = ev.getY();
    }

    if (batchSelected.isEmpty()) {
      return super.onInterceptTouchEvent(ev);
    } else {
      return true;
    }
  }

  @Override
  protected void onDetachedFromWindow() {
    ConversationSwipeAnimationHelper.update(this, 0f, 1f);
    unbind();
    super.onDetachedFromWindow();
  }

  @Override
  public void setEventListener(@Nullable EventListener eventListener) {
    this.eventListener = eventListener;
  }

  public boolean disallowSwipe(float downX, float downY) {
    if (!hasAudio(messageRecord)) return false;

    audioViewStub.get().getSeekBarGlobalVisibleRect(SWIPE_RECT);
    return SWIPE_RECT.contains((int) downX, (int) downY);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    if (isInEditMode()) {
      return;
    }

    boolean needsMeasure = false;

    if (hasQuote(messageRecord)) {
      if (quoteView == null) {
        throw new AssertionError();
      }
      int quoteWidth     = quoteView.getMeasuredWidth();
      int availableWidth = getAvailableMessageBubbleWidth(quoteView);

      if (quoteWidth != availableWidth) {
        quoteView.getLayoutParams().width = availableWidth;
        needsMeasure = true;
      }
    }

    int defaultTopMargin      = readDimen(R.dimen.message_bubble_default_footer_bottom_margin);
    int defaultBottomMargin   = readDimen(R.dimen.message_bubble_bottom_padding);
    int collapsedBottomMargin = readDimen(R.dimen.message_bubble_collapsed_bottom_padding);

    if (!updatingFooter                                                &&
        getActiveFooter(messageRecord) == footer                       &&
        !hasAudio(messageRecord)                                       &&
        isFooterVisible(messageRecord, nextMessageRecord, groupThread) &&
        !bodyText.isJumbomoji()                                        &&
        bodyText.getLastLineWidth() > 0)
    {
      TextView dateView           = footer.getDateView();
      int      footerWidth        = footer.getMeasuredWidth();
      int      availableWidth     = getAvailableMessageBubbleWidth(bodyText);
      int      collapsedTopMargin = -1 * (dateView.getMeasuredHeight() + ViewUtil.dpToPx(4));

      if (bodyText.isSingleLine()) {
        int maxBubbleWidth  = hasBigImageLinkPreview(messageRecord) || hasThumbnail(messageRecord) ? readDimen(R.dimen.media_bubble_max_width) : getMaxBubbleWidth();
        int bodyMargins     = ViewUtil.getLeftMargin(bodyText) + ViewUtil.getRightMargin(bodyText);
        int sizeWithMargins = bodyText.getMeasuredWidth() + ViewUtil.dpToPx(6) + footerWidth + bodyMargins;
        int minSize         = Math.min(maxBubbleWidth, Math.max(bodyText.getMeasuredWidth() + ViewUtil.dpToPx(6) + footerWidth + bodyMargins, bodyBubble.getMeasuredWidth()));

        if (hasQuote(messageRecord) && sizeWithMargins < availableWidth) {
          ViewUtil.setTopMargin(footer, collapsedTopMargin);
          ViewUtil.setBottomMargin(footer, collapsedBottomMargin);
          needsMeasure   = true;
          updatingFooter = true;
        } else if (sizeWithMargins != bodyText.getMeasuredWidth() && sizeWithMargins <= minSize) {
          bodyBubble.getLayoutParams().width = minSize;
          ViewUtil.setTopMargin(footer, collapsedTopMargin);
          ViewUtil.setBottomMargin(footer, collapsedBottomMargin);
          needsMeasure   = true;
          updatingFooter = true;
        }
      }

      if (!updatingFooter && bodyText.getLastLineWidth() + ViewUtil.dpToPx(6) + footerWidth <= bodyText.getMeasuredWidth()) {
        ViewUtil.setTopMargin(footer, collapsedTopMargin);
        ViewUtil.setBottomMargin(footer, collapsedBottomMargin);
        updatingFooter = true;
        needsMeasure   = true;
      }
    }

    if (!updatingFooter && ViewUtil.getTopMargin(footer) != defaultTopMargin) {
      ViewUtil.setTopMargin(footer, defaultTopMargin);
      ViewUtil.setBottomMargin(footer, defaultBottomMargin);
      needsMeasure = true;
    }

    if (hasSharedContact(messageRecord)) {
      int contactWidth   = sharedContactStub.get().getMeasuredWidth();
      int availableWidth = getAvailableMessageBubbleWidth(sharedContactStub.get());

      if (contactWidth != availableWidth) {
        sharedContactStub.get().getLayoutParams().width = availableWidth;
        needsMeasure = true;
      }
    }

    if (hasAudio(messageRecord)) {
      ConversationItemFooter activeFooter   = getActiveFooter(messageRecord);
      int                    availableWidth = getAvailableMessageBubbleWidth(footer);

      if (activeFooter.getVisibility() != GONE && activeFooter.getMeasuredWidth() != availableWidth) {
        activeFooter.getLayoutParams().width = availableWidth;
        needsMeasure = true;
      }
    }

    if (needsMeasure) {
      if (measureCalls < MAX_MEASURE_CALLS) {
        measureCalls++;
        measure(widthMeasureSpec, heightMeasureSpec);
      } else {
        Log.w(TAG, "Hit measure() cap of " + MAX_MEASURE_CALLS);
      }
    } else {
      measureCalls = 0;
      updatingFooter = false;
    }
  }

  @Override
  public void onRecipientChanged(@NonNull Recipient modified) {
    if (conversationRecipient.getId().equals(modified.getId())) {
      setBubbleState(messageRecord, modified, modified.hasWallpaper(), colorizer);

      if (audioViewStub.resolved()) {
        setAudioViewTint(messageRecord);
      }
    }

    if (recipient.getId().equals(modified.getId())) {
      setContactPhoto(modified);
      setGroupMessageStatus(messageRecord, modified);
    }
  }

  private int getAvailableMessageBubbleWidth(@NonNull View forView) {
    int availableWidth;
    if (hasAudio(messageRecord)) {
      availableWidth = audioViewStub.get().getMeasuredWidth() + ViewUtil.getLeftMargin(audioViewStub.get()) + ViewUtil.getRightMargin(audioViewStub.get());
    } else if (!isViewOnceMessage(messageRecord) && (hasThumbnail(messageRecord) || hasBigImageLinkPreview(messageRecord))) {
      availableWidth = mediaThumbnailStub.require().getMeasuredWidth();
    } else {
      availableWidth = bodyBubble.getMeasuredWidth() - bodyBubble.getPaddingLeft() - bodyBubble.getPaddingRight();
    }

    availableWidth -= ViewUtil.getLeftMargin(forView) + ViewUtil.getRightMargin(forView);

    return availableWidth;
  }

  private int getMaxBubbleWidth() {
    int paddings = getPaddingLeft() + getPaddingRight() + ViewUtil.getLeftMargin(bodyBubble) + ViewUtil.getRightMargin(bodyBubble);
    if (groupThread && !messageRecord.isOutgoing() && !messageRecord.isRemoteDelete()) {
      paddings += contactPhoto.getLayoutParams().width + ViewUtil.getLeftMargin(contactPhoto) + ViewUtil.getRightMargin(contactPhoto);
    }
    return getMeasuredWidth() - paddings;
  }

  private void initializeAttributes() {
    defaultBubbleColor             = ContextCompat.getColor(context, R.color.signal_background_secondary);
    defaultBubbleColorForWallpaper = ContextCompat.getColor(context, R.color.conversation_item_wallpaper_bubble_color);
  }

  private @ColorInt int getDefaultBubbleColor(boolean hasWallpaper) {
    return hasWallpaper ? defaultBubbleColorForWallpaper : defaultBubbleColor;
  }

  @Override
  public void unbind() {
    if (recipient != null) {
      recipient.removeForeverObserver(this);
    }
    if (conversationRecipient != null) {
      conversationRecipient.removeForeverObserver(this);
    }
    cancelPulseOutlinerAnimation();
  }

  @Override
  public @NonNull MultiselectPart getMultiselectPartForLatestTouch() {
    MultiselectCollection parts = conversationMessage.getMultiselectCollection();

    if (parts.isSingle()) {
      return parts.asSingle().getSinglePart();
    }

    MultiselectPart top    = parts.asDouble().getTopPart();
    MultiselectPart bottom = parts.asDouble().getBottomPart();

    if (hasThumbnail(messageRecord)) {
      return isTouchBelowBoundary(mediaThumbnailStub.require()) ? bottom : top;
    } else if (hasDocument(messageRecord)) {
      return isTouchBelowBoundary(documentViewStub.get()) ? bottom : top;
    } else if (hasAudio(messageRecord)) {
      return isTouchBelowBoundary(audioViewStub.get()) ? bottom : top;
    } {
      throw new IllegalStateException("Found a situation where we have something other than a thumbnail or a document.");
    }
  }

  private boolean isTouchBelowBoundary(@NonNull View child) {
    Projection childProjection = Projection.relativeToParent(this, child, null);
    float childBoundary = childProjection.getY() + childProjection.getHeight();

    return lastYDownRelativeToThis > childBoundary;
  }

  @Override
  public int getTopBoundaryOfMultiselectPart(@NonNull MultiselectPart multiselectPart) {

    boolean isTextPart       = multiselectPart instanceof MultiselectPart.Text;
    boolean isAttachmentPart = multiselectPart instanceof MultiselectPart.Attachments;

    if (hasThumbnail(messageRecord) && isAttachmentPart) {
      return getProjectionTop(mediaThumbnailStub.require());
    } else if (hasThumbnail(messageRecord) && isTextPart) {
      return getProjectionBottom(mediaThumbnailStub.require());
    } else if (hasDocument(messageRecord) && isAttachmentPart) {
      return getProjectionTop(documentViewStub.get());
    } else if (hasDocument(messageRecord) && isTextPart) {
      return getProjectionBottom(documentViewStub.get());
    } else if (hasAudio(messageRecord) && isAttachmentPart) {
      return getProjectionTop(audioViewStub.get());
    } else if (hasAudio(messageRecord) && isTextPart) {
      return getProjectionBottom(audioViewStub.get());
    } else if (hasNoBubble(messageRecord)) {
      return getTop();
    } else {
      return getProjectionTop(bodyBubble);
    }
  }

  private static int getProjectionTop(@NonNull View child) {
    return (int) Projection.relativeToViewRoot(child, null).getY();
  }

  private static int getProjectionBottom(@NonNull View child) {
    Projection projection = Projection.relativeToViewRoot(child, null);
    return (int) projection.getY() + projection.getHeight();
  }

  @Override
  public int getBottomBoundaryOfMultiselectPart(@NonNull MultiselectPart multiselectPart) {
    if (multiselectPart instanceof MultiselectPart.Attachments && hasThumbnail(messageRecord)) {
      return getProjectionBottom(mediaThumbnailStub.require());
    } else if (multiselectPart instanceof MultiselectPart.Attachments && hasDocument(messageRecord)) {
      return getProjectionBottom(documentViewStub.get());
    } else if (multiselectPart instanceof MultiselectPart.Attachments && hasAudio(messageRecord)) {
      return getProjectionBottom(audioViewStub.get());
    } else if (hasNoBubble(messageRecord)) {
      return getBottom();
    } else {
      return getProjectionBottom(bodyBubble);
    }
  }

  @Override
  public boolean hasNonSelectableMedia() {
    return hasQuote(messageRecord) || hasLinkPreview(messageRecord);
  }

  @Override
  public @NonNull ConversationMessage getConversationMessage() {
    return conversationMessage;
  }

  /// MessageRecord Attribute Parsers

  private void setBubbleState(MessageRecord messageRecord, @NonNull Recipient recipient, boolean hasWallpaper, @NonNull Colorizer colorizer) {
    this.hasWallpaper = hasWallpaper;

    ViewUtil.updateLayoutParams(bodyBubble, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    bodyText.setTextColor(ContextCompat.getColor(getContext(), R.color.signal_text_primary));
    bodyText.setLinkTextColor(ContextCompat.getColor(getContext(), R.color.signal_text_primary));

    if (messageRecord.isOutgoing() && !messageRecord.isRemoteDelete()) {
      bodyBubble.getBackground().setColorFilter(recipient.getChatColors().getChatBubbleColorFilter());
      bodyText.setTextColor(colorizer.getOutgoingBodyTextColor(context));
      bodyText.setLinkTextColor(colorizer.getOutgoingBodyTextColor(context));
      footer.setTextColor(colorizer.getOutgoingFooterTextColor(context));
      footer.setIconColor(colorizer.getOutgoingFooterIconColor(context));
      footer.setRevealDotColor(colorizer.getOutgoingFooterIconColor(context));
      footer.setOnlyShowSendingStatus(false, messageRecord);
    } else if (messageRecord.isRemoteDelete()) {
      if (hasWallpaper) {
        bodyBubble.getBackground().setColorFilter(ContextCompat.getColor(context, R.color.wallpaper_bubble_color), PorterDuff.Mode.SRC_IN);
      } else {
        bodyBubble.getBackground().setColorFilter(ContextCompat.getColor(context, R.color.signal_background_primary), PorterDuff.Mode.MULTIPLY);
        footer.setIconColor(ContextCompat.getColor(context, R.color.signal_icon_tint_secondary));
        footer.setRevealDotColor(ContextCompat.getColor(context, R.color.signal_icon_tint_secondary));
      }
      footer.setTextColor(ContextCompat.getColor(context, R.color.signal_text_secondary));
      footer.setOnlyShowSendingStatus(messageRecord.isRemoteDelete(), messageRecord);
    } else {
      bodyBubble.getBackground().setColorFilter(getDefaultBubbleColor(hasWallpaper), PorterDuff.Mode.SRC_IN);
      footer.setTextColor(ContextCompat.getColor(context, R.color.signal_text_secondary));
      footer.setIconColor(ContextCompat.getColor(context, R.color.signal_text_secondary));
      footer.setRevealDotColor(ContextCompat.getColor(context, R.color.signal_text_secondary));
      footer.setOnlyShowSendingStatus(false, messageRecord);
    }

    outliner.setColor(ContextCompat.getColor(context, R.color.signal_text_secondary));

    pulseOutliner.setColor(ContextCompat.getColor(getContext(), R.color.signal_inverse_transparent));
    pulseOutliner.setStrokeWidth(ViewUtil.dpToPx(4));

    outliners.clear();
    if (shouldDrawBodyBubbleOutline(messageRecord, hasWallpaper)) {
      outliners.add(outliner);
    }
    outliners.add(pulseOutliner);

    bodyBubble.setOutliners(outliners);

    if (mediaThumbnailStub.resolved()) {
      mediaThumbnailStub.require().setPulseOutliner(pulseOutliner);
    }

    if (audioViewStub.resolved()) {
      setAudioViewTint(messageRecord);
    }

    if (hasWallpaper) {
      replyIcon.setBackgroundResource(R.drawable.wallpaper_message_decoration_background);
    } else {
      replyIcon.setBackground(null);
    }
  }

  private void setAudioViewTint(MessageRecord messageRecord) {
    if (hasAudio(messageRecord)) {
      if (!messageRecord.isOutgoing()) {
        audioViewStub.get().setTint(getContext().getResources().getColor(R.color.conversation_item_incoming_audio_foreground_tint));
        if (hasWallpaper) {
          audioViewStub.get().setProgressAndPlayBackgroundTint(getContext().getResources().getColor(R.color.conversation_item_incoming_audio_play_pause_background_tint_wallpaper));
        } else {
          audioViewStub.get().setProgressAndPlayBackgroundTint(getContext().getResources().getColor(R.color.conversation_item_incoming_audio_play_pause_background_tint_normal));
        }
      } else {
        audioViewStub.get().setTint(Color.WHITE);
        audioViewStub.get().setProgressAndPlayBackgroundTint(getContext().getResources().getColor(R.color.transparent_white_20));
      }
    }
  }

  private void setInteractionState(ConversationMessage conversationMessage, boolean pulseMention) {
    Set<MultiselectPart> multiselectParts  = conversationMessage.getMultiselectCollection().toSet();
    boolean              isMessageSelected = Util.hasItems(Sets.intersection(multiselectParts, batchSelected));

    if (isMessageSelected) {
      setSelected(true);
    } else if (pulseMention) {
      setSelected(false);
      startPulseOutlinerAnimation();
    } else {
      setSelected(false);
    }

    if (mediaThumbnailStub.resolved()) {
      mediaThumbnailStub.require().setFocusable(!shouldInterceptClicks(conversationMessage.getMessageRecord()) && batchSelected.isEmpty());
      mediaThumbnailStub.require().setClickable(!shouldInterceptClicks(conversationMessage.getMessageRecord()) && batchSelected.isEmpty());
      mediaThumbnailStub.require().setLongClickable(batchSelected.isEmpty());
    }

    if (audioViewStub.resolved()) {
      audioViewStub.get().setFocusable(!shouldInterceptClicks(conversationMessage.getMessageRecord()) && batchSelected.isEmpty());
      audioViewStub.get().setClickable(batchSelected.isEmpty());
      audioViewStub.get().setEnabled(batchSelected.isEmpty());
    }

    if (documentViewStub.resolved()) {
      documentViewStub.get().setFocusable(!shouldInterceptClicks(conversationMessage.getMessageRecord()) && batchSelected.isEmpty());
      documentViewStub.get().setClickable(batchSelected.isEmpty());
    }
  }

  private void startPulseOutlinerAnimation() {
    pulseOutlinerAlphaAnimator = ValueAnimator.ofInt(0, 0x66, 0).setDuration(600);
    pulseOutlinerAlphaAnimator.addUpdateListener(animator -> {
      pulseOutliner.setAlpha((Integer) animator.getAnimatedValue());
      bodyBubble.invalidate();

      if (mediaThumbnailStub.resolved()) {
        mediaThumbnailStub.require().invalidate();
      }
    });
    pulseOutlinerAlphaAnimator.start();
  }

  private void cancelPulseOutlinerAnimation() {
    if (pulseOutlinerAlphaAnimator != null) {
      pulseOutlinerAlphaAnimator.cancel();
      pulseOutlinerAlphaAnimator = null;
    }

    pulseOutliner.setAlpha(0);
  }

  private boolean shouldDrawBodyBubbleOutline(MessageRecord messageRecord, boolean hasWallpaper) {
    if (hasWallpaper) {
      return false;
    } else {
      return messageRecord.isRemoteDelete();
    }
  }

  private boolean isCaptionlessMms(MessageRecord messageRecord) {
    return MessageRecordUtil.isCaptionlessMms(messageRecord, context);
  }

  private boolean hasAudio(MessageRecord messageRecord) {
    return MessageRecordUtil.hasAudio(messageRecord);
  }

  private boolean hasThumbnail(MessageRecord messageRecord) {
    return MessageRecordUtil.hasThumbnail(messageRecord);
  }

  private boolean hasSticker(MessageRecord messageRecord) {
    return MessageRecordUtil.hasSticker(messageRecord);
  }

  private boolean isBorderless(MessageRecord messageRecord) {
    return MessageRecordUtil.isBorderless(messageRecord, context);
  }

  private boolean hasNoBubble(MessageRecord messageRecord) {
    return MessageRecordUtil.hasNoBubble(messageRecord, context);
  }

  private boolean hasOnlyThumbnail(MessageRecord messageRecord) {
    return MessageRecordUtil.hasOnlyThumbnail(messageRecord, context);
  }

  private boolean hasDocument(MessageRecord messageRecord) {
    return MessageRecordUtil.hasDocument(messageRecord);
  }

  private boolean hasExtraText(MessageRecord messageRecord) {
    return MessageRecordUtil.hasExtraText(messageRecord);
  }

  private boolean hasQuote(MessageRecord messageRecord) {
    return MessageRecordUtil.hasQuote(messageRecord);
  }

  private boolean hasSharedContact(MessageRecord messageRecord) {
    return MessageRecordUtil.hasSharedContact(messageRecord);
  }

  private boolean hasLinkPreview(MessageRecord  messageRecord) {
    return MessageRecordUtil.hasLinkPreview(messageRecord);
  }

  private boolean hasBigImageLinkPreview(MessageRecord messageRecord) {
    return MessageRecordUtil.hasBigImageLinkPreview(messageRecord, context);
  }

  private boolean isViewOnceMessage(MessageRecord messageRecord) {
    return MessageRecordUtil.isViewOnceMessage(messageRecord);
  }

  private void setBodyText(@NonNull MessageRecord messageRecord,
                           @Nullable String searchQuery,
                           boolean messageRequestAccepted)
  {
    bodyText.setClickable(false);
    bodyText.setFocusable(false);
    bodyText.setTextSize(TypedValue.COMPLEX_UNIT_SP, SignalStore.settings().getMessageFontSize());
    bodyText.setMovementMethod(LongClickMovementMethod.getInstance(getContext()));

    if (messageRecord.isRemoteDelete()) {
      String deletedMessage = context.getString(messageRecord.isOutgoing() ? R.string.ConversationItem_you_deleted_this_message : R.string.ConversationItem_this_message_was_deleted);
      SpannableString italics = new SpannableString(deletedMessage);
      italics.setSpan(new StyleSpan(android.graphics.Typeface.ITALIC), 0, deletedMessage.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
      italics.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.signal_text_primary)),
                                              0,
                                              deletedMessage.length(),
                                              Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

      bodyText.setText(italics);
      bodyText.setVisibility(View.VISIBLE);
      bodyText.setOverflowText(null);
    } else if (isCaptionlessMms(messageRecord)) {
      bodyText.setVisibility(View.GONE);
    } else {
      Spannable styledText = conversationMessage.getDisplayBody(getContext());
      if (messageRequestAccepted) {
        linkifyMessageBody(styledText, batchSelected.isEmpty());
      }
      styledText = SearchUtil.getHighlightedSpan(locale, () -> new BackgroundColorSpan(Color.YELLOW), styledText, searchQuery, SearchUtil.STRICT);
      styledText = SearchUtil.getHighlightedSpan(locale, () -> new ForegroundColorSpan(Color.BLACK), styledText, searchQuery, SearchUtil.STRICT);

      if (hasExtraText(messageRecord)) {
        bodyText.setOverflowText(getLongMessageSpan(messageRecord));
      } else {
        bodyText.setOverflowText(null);
      }

      if (messageRecord.isOutgoing()) {
        bodyText.setMentionBackgroundTint(ContextCompat.getColor(context, R.color.transparent_black_25));
      } else {
        bodyText.setMentionBackgroundTint(ContextCompat.getColor(context, ThemeUtil.isDarkTheme(context) ? R.color.core_grey_60 : R.color.core_grey_20));
      }

      bodyText.setText(StringUtil.trim(styledText));
      bodyText.setVisibility(View.VISIBLE);
    }
  }

  private void setMediaAttributes(@NonNull  MessageRecord                messageRecord,
                                  @NonNull  Optional<MessageRecord>      previousRecord,
                                  @NonNull  Optional<MessageRecord>      nextRecord,
                                            boolean                      isGroupThread,
                                            boolean                      hasWallpaper,
                                            boolean                      messageRequestAccepted,
                                            boolean                      allowedToPlayInline)
  {
    boolean showControls = !messageRecord.isFailed();

    ViewUtil.setTopMargin(bodyText, readDimen(R.dimen.message_bubble_top_padding));

    bodyBubble.setQuoteViewProjection(null);
    bodyBubble.setVideoPlayerProjection(null);

    if (eventListener != null && audioViewStub.resolved()) {
      Log.d(TAG, "setMediaAttributes: unregistering voice note callbacks for audio slide " + audioViewStub.get().getAudioSlideUri());
      eventListener.onUnregisterVoiceNoteCallbacks(audioViewStub.get().getPlaybackStateObserver());
    }

    footer.setPlaybackSpeedListener(null);

    if (isViewOnceMessage(messageRecord) && !messageRecord.isRemoteDelete()) {
      revealableStub.get().setVisibility(VISIBLE);
      if (mediaThumbnailStub.resolved()) mediaThumbnailStub.require().setVisibility(View.GONE);
      if (audioViewStub.resolved())      audioViewStub.get().setVisibility(View.GONE);
      if (documentViewStub.resolved())   documentViewStub.get().setVisibility(View.GONE);
      if (sharedContactStub.resolved())  sharedContactStub.get().setVisibility(GONE);
      if (linkPreviewStub.resolved())    linkPreviewStub.get().setVisibility(GONE);
      if (stickerStub.resolved())        stickerStub.get().setVisibility(View.GONE);

      revealableStub.get().setMessage((MmsMessageRecord) messageRecord, hasWallpaper);
      revealableStub.get().setOnClickListener(revealableClickListener);
      revealableStub.get().setOnLongClickListener(passthroughClickListener);

      updateRevealableMargins(messageRecord, previousRecord, nextRecord, isGroupThread);

      footer.setVisibility(VISIBLE);
    } else if (hasSharedContact(messageRecord)) {
      sharedContactStub.get().setVisibility(VISIBLE);
      if (audioViewStub.resolved())      audioViewStub.get().setVisibility(View.GONE);
      if (mediaThumbnailStub.resolved()) mediaThumbnailStub.require().setVisibility(View.GONE);
      if (documentViewStub.resolved())   documentViewStub.get().setVisibility(View.GONE);
      if (linkPreviewStub.resolved())    linkPreviewStub.get().setVisibility(GONE);
      if (stickerStub.resolved())        stickerStub.get().setVisibility(View.GONE);
      if (revealableStub.resolved())     revealableStub.get().setVisibility(View.GONE);

      sharedContactStub.get().setContact(((MediaMmsMessageRecord) messageRecord).getSharedContacts().get(0), glideRequests, locale);
      sharedContactStub.get().setEventListener(sharedContactEventListener);
      sharedContactStub.get().setOnClickListener(sharedContactClickListener);
      sharedContactStub.get().setOnLongClickListener(passthroughClickListener);

      setSharedContactCorners(messageRecord, previousRecord, nextRecord, isGroupThread);

      ViewUtil.updateLayoutParams(bodyText, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      ViewUtil.updateLayoutParamsIfNonNull(groupSenderHolder, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      footer.setVisibility(GONE);
    } else if (hasLinkPreview(messageRecord) && messageRequestAccepted) {
      linkPreviewStub.get().setVisibility(View.VISIBLE);
      if (audioViewStub.resolved())      audioViewStub.get().setVisibility(View.GONE);
      if (mediaThumbnailStub.resolved()) mediaThumbnailStub.require().setVisibility(View.GONE);
      if (documentViewStub.resolved())   documentViewStub.get().setVisibility(View.GONE);
      if (sharedContactStub.resolved())  sharedContactStub.get().setVisibility(GONE);
      if (stickerStub.resolved())        stickerStub.get().setVisibility(View.GONE);
      if (revealableStub.resolved())     revealableStub.get().setVisibility(View.GONE);

      //noinspection ConstantConditions
      LinkPreview linkPreview = ((MmsMessageRecord) messageRecord).getLinkPreviews().get(0);

      if (hasBigImageLinkPreview(messageRecord)) {
        mediaThumbnailStub.require().setVisibility(VISIBLE);
        mediaThumbnailStub.require().setMinimumThumbnailWidth(readDimen(R.dimen.media_bubble_min_width_with_content));
        mediaThumbnailStub.require().setImageResource(glideRequests, Collections.singletonList(new ImageSlide(context, linkPreview.getThumbnail().get())), showControls, false);
        mediaThumbnailStub.require().setThumbnailClickListener(new LinkPreviewThumbnailClickListener());
        mediaThumbnailStub.require().setDownloadClickListener(downloadClickListener);
        mediaThumbnailStub.require().setOnLongClickListener(passthroughClickListener);

        linkPreviewStub.get().setLinkPreview(glideRequests, linkPreview, false);

        setThumbnailCorners(messageRecord, previousRecord, nextRecord, isGroupThread);
        setLinkPreviewCorners(messageRecord, previousRecord, nextRecord, isGroupThread, true);

        ViewUtil.updateLayoutParams(bodyText, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ViewUtil.updateLayoutParamsIfNonNull(groupSenderHolder, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ViewUtil.setTopMargin(linkPreviewStub.get(), 0);
      } else {
        linkPreviewStub.get().setLinkPreview(glideRequests, linkPreview, true);
        linkPreviewStub.get().setDownloadClickedListener(downloadClickListener);
        setLinkPreviewCorners(messageRecord, previousRecord, nextRecord, isGroupThread, false);
        ViewUtil.updateLayoutParams(bodyText, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ViewUtil.updateLayoutParamsIfNonNull(groupSenderHolder, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        //noinspection ConstantConditions
        int topMargin = isGroupThread && isStartOfMessageCluster(messageRecord, previousRecord, isGroupThread) && !messageRecord.isOutgoing() ? readDimen(R.dimen.message_bubble_top_padding) : 0;
        ViewUtil.setTopMargin(linkPreviewStub.get(), topMargin);
      }

      linkPreviewStub.get().setOnClickListener(linkPreviewClickListener);
      linkPreviewStub.get().setOnLongClickListener(passthroughClickListener);

      footer.setVisibility(VISIBLE);
    } else if (hasAudio(messageRecord)) {
      audioViewStub.get().setVisibility(View.VISIBLE);
      if (mediaThumbnailStub.resolved()) mediaThumbnailStub.require().setVisibility(View.GONE);
      if (documentViewStub.resolved())   documentViewStub.get().setVisibility(View.GONE);
      if (sharedContactStub.resolved())  sharedContactStub.get().setVisibility(GONE);
      if (linkPreviewStub.resolved())    linkPreviewStub.get().setVisibility(GONE);
      if (stickerStub.resolved())        stickerStub.get().setVisibility(View.GONE);
      if (revealableStub.resolved())     revealableStub.get().setVisibility(View.GONE);

      audioViewStub.get().setAudio(Objects.requireNonNull(((MediaMmsMessageRecord) messageRecord).getSlideDeck().getAudioSlide()), new AudioViewCallbacks(), showControls, true);
      audioViewStub.get().setDownloadClickListener(singleDownloadClickListener);
      audioViewStub.get().setOnLongClickListener(passthroughClickListener);

      if (eventListener != null) {
        Log.d(TAG, "setMediaAttributes: registered listener for audio slide " + audioViewStub.get().getAudioSlideUri());
        eventListener.onRegisterVoiceNoteCallbacks(audioViewStub.get().getPlaybackStateObserver());
      } else {
        Log.w(TAG, "setMediaAttributes: could not register listener for audio slide " + audioViewStub.get().getAudioSlideUri());
      }

      ViewUtil.updateLayoutParams(bodyText, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      ViewUtil.updateLayoutParamsIfNonNull(groupSenderHolder, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

      footer.setPlaybackSpeedListener(new AudioPlaybackSpeedToggleListener());
      footer.setVisibility(VISIBLE);
    } else if (hasDocument(messageRecord)) {
      documentViewStub.get().setVisibility(View.VISIBLE);
      if (mediaThumbnailStub.resolved()) mediaThumbnailStub.require().setVisibility(View.GONE);
      if (audioViewStub.resolved())      audioViewStub.get().setVisibility(View.GONE);
      if (sharedContactStub.resolved())  sharedContactStub.get().setVisibility(GONE);
      if (linkPreviewStub.resolved())    linkPreviewStub.get().setVisibility(GONE);
      if (stickerStub.resolved())        stickerStub.get().setVisibility(View.GONE);
      if (revealableStub.resolved())     revealableStub.get().setVisibility(View.GONE);

      //noinspection ConstantConditions
      documentViewStub.get().setDocument(((MediaMmsMessageRecord) messageRecord).getSlideDeck().getDocumentSlide(), showControls);
      documentViewStub.get().setDocumentClickListener(new ThumbnailClickListener());
      documentViewStub.get().setDownloadClickListener(singleDownloadClickListener);
      documentViewStub.get().setOnLongClickListener(passthroughClickListener);

      ViewUtil.updateLayoutParams(bodyText, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      ViewUtil.updateLayoutParamsIfNonNull(groupSenderHolder, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      ViewUtil.setTopMargin(bodyText, 0);

      footer.setVisibility(VISIBLE);
    } else if ((hasSticker(messageRecord) && isCaptionlessMms(messageRecord)) || isBorderless(messageRecord)) {
      bodyBubble.setBackgroundColor(Color.TRANSPARENT);

      stickerStub.get().setVisibility(View.VISIBLE);
      if (mediaThumbnailStub.resolved()) mediaThumbnailStub.require().setVisibility(View.GONE);
      if (audioViewStub.resolved())      audioViewStub.get().setVisibility(View.GONE);
      if (documentViewStub.resolved())   documentViewStub.get().setVisibility(View.GONE);
      if (sharedContactStub.resolved())  sharedContactStub.get().setVisibility(GONE);
      if (linkPreviewStub.resolved())    linkPreviewStub.get().setVisibility(GONE);
      if (revealableStub.resolved())     revealableStub.get().setVisibility(View.GONE);

      if (hasSticker(messageRecord)) {
        //noinspection ConstantConditions
        stickerStub.get().setSlide(glideRequests, ((MmsMessageRecord) messageRecord).getSlideDeck().getStickerSlide());
        stickerStub.get().setThumbnailClickListener(new StickerClickListener());
      } else {
        //noinspection ConstantConditions
        stickerStub.get().setSlide(glideRequests, ((MmsMessageRecord) messageRecord).getSlideDeck().getThumbnailSlide());
        stickerStub.get().setThumbnailClickListener((v, slide) -> performClick());
      }

      stickerStub.get().setDownloadClickListener(downloadClickListener);
      stickerStub.get().setOnLongClickListener(passthroughClickListener);
      stickerStub.get().setOnClickListener(passthroughClickListener);

      ViewUtil.updateLayoutParams(bodyText, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      ViewUtil.updateLayoutParamsIfNonNull(groupSenderHolder, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

      footer.setVisibility(VISIBLE);
    } else if (hasThumbnail(messageRecord)) {
      mediaThumbnailStub.require().setVisibility(View.VISIBLE);
      if (audioViewStub.resolved())     audioViewStub.get().setVisibility(View.GONE);
      if (documentViewStub.resolved())  documentViewStub.get().setVisibility(View.GONE);
      if (sharedContactStub.resolved()) sharedContactStub.get().setVisibility(GONE);
      if (linkPreviewStub.resolved())   linkPreviewStub.get().setVisibility(GONE);
      if (stickerStub.resolved())       stickerStub.get().setVisibility(View.GONE);
      if (revealableStub.resolved())    revealableStub.get().setVisibility(View.GONE);

      List<Slide> thumbnailSlides = ((MmsMessageRecord) messageRecord).getSlideDeck().getThumbnailSlides();
      mediaThumbnailStub.require().setMinimumThumbnailWidth(readDimen(isCaptionlessMms(messageRecord) ? R.dimen.media_bubble_min_width_solo
                                                                                                  : R.dimen.media_bubble_min_width_with_content));
      mediaThumbnailStub.require().setImageResource(glideRequests,
                                                    thumbnailSlides,
                                                    showControls,
                                                    false);
      mediaThumbnailStub.require().setThumbnailClickListener(new ThumbnailClickListener());
      mediaThumbnailStub.require().setDownloadClickListener(downloadClickListener);
      mediaThumbnailStub.require().setOnLongClickListener(passthroughClickListener);
      mediaThumbnailStub.require().setOnClickListener(passthroughClickListener);
      mediaThumbnailStub.require().showShade(TextUtils.isEmpty(messageRecord.getDisplayBody(getContext())) && !hasExtraText(messageRecord));

      if (!messageRecord.isOutgoing()) {
        mediaThumbnailStub.require().setConversationColor(getDefaultBubbleColor(hasWallpaper));
      } else {
        mediaThumbnailStub.require().setConversationColor(Color.TRANSPARENT);
      }

      mediaThumbnailStub.require().setBorderless(false);

      setThumbnailCorners(messageRecord, previousRecord, nextRecord, isGroupThread);

      ViewUtil.updateLayoutParams(bodyText, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      ViewUtil.updateLayoutParamsIfNonNull(groupSenderHolder, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

      footer.setVisibility(VISIBLE);

      if (thumbnailSlides.size() == 1          &&
          thumbnailSlides.get(0).isVideoGif()  &&
          thumbnailSlides.get(0) instanceof VideoSlide)
      {
        canPlayContent = GiphyMp4PlaybackPolicy.autoplay() || allowedToPlayInline;

        Uri uri = thumbnailSlides.get(0).getUri();
        if (uri != null) {
          mediaItem = MediaItem.fromUri(uri);
        } else {
          mediaItem = null;
        }
      }

    } else {
      if (mediaThumbnailStub.resolved()) mediaThumbnailStub.require().setVisibility(View.GONE);
      if (audioViewStub.resolved())      audioViewStub.get().setVisibility(View.GONE);
      if (documentViewStub.resolved())   documentViewStub.get().setVisibility(View.GONE);
      if (sharedContactStub.resolved())  sharedContactStub.get().setVisibility(GONE);
      if (linkPreviewStub.resolved())    linkPreviewStub.get().setVisibility(GONE);
      if (stickerStub.resolved())        stickerStub.get().setVisibility(View.GONE);
      if (revealableStub.resolved())     revealableStub.get().setVisibility(View.GONE);

      ViewUtil.updateLayoutParams(bodyText, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      ViewUtil.updateLayoutParamsIfNonNull(groupSenderHolder, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

      footer.setVisibility(VISIBLE);

      //noinspection ConstantConditions
      int topMargin = !messageRecord.isOutgoing() && isGroupThread && isStartOfMessageCluster(messageRecord, previousRecord, isGroupThread)
                      ? readDimen(R.dimen.message_bubble_text_only_top_margin)
                      : readDimen(R.dimen.message_bubble_top_padding);
      ViewUtil.setTopMargin(bodyText, topMargin);
    }
  }

  private void updateRevealableMargins(MessageRecord messageRecord, Optional<MessageRecord> previous, Optional<MessageRecord> next, boolean isGroupThread) {
    int bigMargin = readDimen(R.dimen.message_bubble_revealable_padding);
    int smallMargin = readDimen(R.dimen.message_bubble_top_padding);

    //noinspection ConstantConditions
    if (messageRecord.isOutgoing() || !isStartOfMessageCluster(messageRecord, previous, isGroupThread)) {
      ViewUtil.setTopMargin(revealableStub.get(), bigMargin);
    } else {
      ViewUtil.setTopMargin(revealableStub.get(), smallMargin);
    }

    if (isFooterVisible(messageRecord, next, isGroupThread)) {
      ViewUtil.setBottomMargin(revealableStub.get(), smallMargin);
    } else {
      ViewUtil.setBottomMargin(revealableStub.get(), bigMargin);
    }
  }

  private void setThumbnailCorners(@NonNull MessageRecord           current,
                                   @NonNull Optional<MessageRecord> previous,
                                   @NonNull Optional<MessageRecord> next,
                                            boolean                 isGroupThread)
  {
    int defaultRadius  = readDimen(R.dimen.message_corner_radius);
    int collapseRadius = readDimen(R.dimen.message_corner_collapse_radius);

    int topStart    = defaultRadius;
    int topEnd      = defaultRadius;
    int bottomStart = defaultRadius;
    int bottomEnd   = defaultRadius;

    if (isSingularMessage(current, previous, next, isGroupThread)) {
      topStart    = defaultRadius;
      topEnd      = defaultRadius;
      bottomStart = defaultRadius;
      bottomEnd   = defaultRadius;
    } else if (isStartOfMessageCluster(current, previous, isGroupThread)) {
      if (current.isOutgoing()) {
        bottomEnd = collapseRadius;
      } else {
        bottomStart = collapseRadius;
      }
    } else if (isEndOfMessageCluster(current, next, isGroupThread)) {
      if (current.isOutgoing()) {
        topEnd = collapseRadius;
      } else {
        topStart = collapseRadius;
      }
    } else {
      if (current.isOutgoing()) {
        topEnd    = collapseRadius;
        bottomEnd = collapseRadius;
      } else {
        topStart    = collapseRadius;
        bottomStart = collapseRadius;
      }
    }

    if (!TextUtils.isEmpty(current.getDisplayBody(getContext()))) {
      bottomStart = 0;
      bottomEnd   = 0;
    }

    if (isStartOfMessageCluster(current, previous, isGroupThread) && !current.isOutgoing() && isGroupThread) {
      topStart = 0;
      topEnd   = 0;
    }

    if (hasQuote(messageRecord)) {
      topStart = 0;
      topEnd   = 0;
    }

    if (hasLinkPreview(messageRecord) || hasExtraText(messageRecord)) {
      bottomStart = 0;
      bottomEnd   = 0;
    }

    if (ViewUtil.isRtl(this)) {
      mediaThumbnailStub.require().setCorners(topEnd, topStart, bottomStart, bottomEnd);
    } else {
      mediaThumbnailStub.require().setCorners(topStart, topEnd, bottomEnd, bottomStart);
    }
  }

  private void setSharedContactCorners(@NonNull MessageRecord current, @NonNull Optional<MessageRecord> previous, @NonNull Optional<MessageRecord> next, boolean isGroupThread) {
    if (TextUtils.isEmpty(messageRecord.getDisplayBody(getContext()))){
      if (isSingularMessage(current, previous, next, isGroupThread) || isEndOfMessageCluster(current, next, isGroupThread)) {
          sharedContactStub.get().setSingularStyle();
      } else if (current.isOutgoing()) {
          sharedContactStub.get().setClusteredOutgoingStyle();
      } else {
          sharedContactStub.get().setClusteredIncomingStyle();
      }
    }
  }

  private void setLinkPreviewCorners(@NonNull MessageRecord current, @NonNull Optional<MessageRecord> previous, @NonNull Optional<MessageRecord> next, boolean isGroupThread, boolean bigImage) {
    int defaultRadius  = readDimen(R.dimen.message_corner_radius);
    int collapseRadius = readDimen(R.dimen.message_corner_collapse_radius);

    if (bigImage || hasQuote(current)) {
      linkPreviewStub.get().setCorners(0, 0);
    } else if (isStartOfMessageCluster(current, previous, isGroupThread) && !current.isOutgoing() && isGroupThread) {
      linkPreviewStub.get().setCorners(0, 0);
    } else if (isSingularMessage(current, previous, next, isGroupThread) || isStartOfMessageCluster(current, previous, isGroupThread)) {
      linkPreviewStub.get().setCorners(defaultRadius, defaultRadius);
    } else if (current.isOutgoing()) {
      linkPreviewStub.get().setCorners(defaultRadius, collapseRadius);
    } else {
      linkPreviewStub.get().setCorners(collapseRadius, defaultRadius);
    }
  }

  private void setContactPhoto(@NonNull Recipient recipient) {
    if (contactPhoto == null) return;

    final RecipientId recipientId = recipient.getId();

    contactPhoto.setOnClickListener(v -> {
      if (eventListener != null) {
        eventListener.onGroupMemberClicked(recipientId, conversationRecipient.get().requireGroupId());
      }
    });

    contactPhoto.setAvatar(glideRequests, recipient, false);
  }

  private void linkifyMessageBody(@NonNull Spannable messageBody,
                                  boolean shouldLinkifyAllLinks)
  {
    int     linkPattern = Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES | Linkify.PHONE_NUMBERS;
    boolean hasLinks    = LinkifyCompat.addLinks(messageBody, shouldLinkifyAllLinks ? linkPattern : 0);

    if (hasLinks) {
      Stream.of(messageBody.getSpans(0, messageBody.length(), URLSpan.class))
            .filterNot(url -> LinkPreviewUtil.isLegalUrl(url.getURL()))
            .forEach(messageBody::removeSpan);

      URLSpan[] urlSpans = messageBody.getSpans(0, messageBody.length(), URLSpan.class);

      for (URLSpan urlSpan : urlSpans) {
        int     start = messageBody.getSpanStart(urlSpan);
        int     end   = messageBody.getSpanEnd(urlSpan);
        URLSpan span  = new InterceptableLongClickCopyLinkSpan(urlSpan.getURL(), urlClickListener);
        messageBody.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
      }
    }

    List<Annotation> mentionAnnotations = MentionAnnotation.getMentionAnnotations(messageBody);
    for (Annotation annotation : mentionAnnotations) {
      messageBody.setSpan(new MentionClickableSpan(RecipientId.from(annotation.getValue())), messageBody.getSpanStart(annotation), messageBody.getSpanEnd(annotation), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
  }

  private void setStatusIcons(MessageRecord messageRecord, boolean hasWallpaper) {
    bodyText.setCompoundDrawablesWithIntrinsicBounds(0, 0, messageRecord.isKeyExchange() ? R.drawable.ic_menu_login : 0, 0);

    if (messageRecord.isFailed()) {
      alertView.setFailed();
    } else if (messageRecord.isPendingInsecureSmsFallback()) {
      alertView.setPendingApproval();
    } else if (messageRecord.isRateLimited()) {
      alertView.setRateLimited();
    } else {
      alertView.setNone();
    }

    if (hasWallpaper) {
      alertView.setBackgroundResource(R.drawable.wallpaper_message_decoration_background);
    } else {
      alertView.setBackground(null);
    }
  }

  private void setQuote(@NonNull MessageRecord current, @NonNull Optional<MessageRecord> previous, @NonNull Optional<MessageRecord> next, boolean isGroupThread, @NonNull ChatColors chatColors) {
    boolean startOfCluster = isStartOfMessageCluster(current, previous, isGroupThread);
    if (current.isMms() && !current.isMmsNotification() && ((MediaMmsMessageRecord)current).getQuote() != null) {
      if (quoteView == null) {
        throw new AssertionError();
      }
      Quote quote = ((MediaMmsMessageRecord)current).getQuote();
      //noinspection ConstantConditions
      quoteView.setQuote(glideRequests, quote.getId(), Recipient.live(quote.getAuthor()).get(), quote.getDisplayText(), quote.isOriginalMissing(), quote.getAttachment(), chatColors);
      quoteView.setVisibility(View.VISIBLE);
      quoteView.setTextSize(TypedValue.COMPLEX_UNIT_SP, SignalStore.settings().getMessageFontSize());
      quoteView.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;

      quoteView.setOnClickListener(view -> {
        if (eventListener != null && batchSelected.isEmpty()) {
          eventListener.onQuoteClicked((MmsMessageRecord) current);
        } else {
          passthroughClickListener.onClick(view);
        }
      });

      quoteView.setOnLongClickListener(passthroughClickListener);

      if (startOfCluster) {
        if (current.isOutgoing()) {
          quoteView.setTopCornerSizes(true, true);
        } else if (isGroupThread) {
          quoteView.setTopCornerSizes(false, false);
        } else {
          quoteView.setTopCornerSizes(true, true);
        }
      } else if (!isSingularMessage(current, previous, next, isGroupThread)) {
        if (current.isOutgoing()) {
          quoteView.setTopCornerSizes(true, false);
        } else {
          quoteView.setTopCornerSizes(false, true);
        }
      }

      if (mediaThumbnailStub.resolved()) {
        ViewUtil.setTopMargin(mediaThumbnailStub.require(), readDimen(R.dimen.message_bubble_top_padding));
      }

      if (linkPreviewStub.resolved() && !hasBigImageLinkPreview(current)) {
        ViewUtil.setTopMargin(linkPreviewStub.get(), readDimen(R.dimen.message_bubble_top_padding));
      }
    } else {
      if (quoteView != null) {
        quoteView.dismiss();
      }

      int topMargin = (current.isOutgoing() || !startOfCluster || !groupThread) ? 0 : readDimen(R.dimen.message_bubble_top_image_margin);
      if (mediaThumbnailStub.resolved()) {
        ViewUtil.setTopMargin(mediaThumbnailStub.require(), topMargin);
      }
    }
  }

  private void setGutterSizes(@NonNull MessageRecord current, boolean isGroupThread) {
    if (isGroupThread && current.isOutgoing()) {
      ViewUtil.setPaddingStart(this, readDimen(R.dimen.conversation_group_left_gutter));
      ViewUtil.setPaddingEnd(this, readDimen(R.dimen.conversation_individual_right_gutter));
    } else if (current.isOutgoing()) {
      ViewUtil.setPaddingStart(this, readDimen(R.dimen.conversation_individual_left_gutter));
      ViewUtil.setPaddingEnd(this, readDimen(R.dimen.conversation_individual_right_gutter));
    }
  }

  private void setReactions(@NonNull MessageRecord current) {
    bodyBubble.setOnSizeChangedListener(null);

    if (current.getReactions().isEmpty()) {
      reactionsView.clear();
      return;
    }

    setReactionsWithWidth(current, bodyBubble.getWidth());
    bodyBubble.setOnSizeChangedListener((width, height) -> setReactionsWithWidth(current, width));
  }

  private void setReactionsWithWidth(@NonNull MessageRecord current, int width) {
    reactionsView.setReactions(current.getReactions(), width);
    reactionsView.setOnClickListener(v -> {
      if (eventListener == null) return;

      eventListener.onReactionClicked(this, current.getId(), current.isMms());
    });
  }

  private void setFooter(@NonNull MessageRecord current, @NonNull Optional<MessageRecord> next, @NonNull Locale locale, boolean isGroupThread, boolean hasWallpaper) {
    ViewUtil.updateLayoutParams(footer, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    ViewUtil.setTopMargin(footer, readDimen(R.dimen.message_bubble_default_footer_bottom_margin));

    footer.setVisibility(GONE);
    ViewUtil.setVisibilityIfNonNull(stickerFooter, GONE);
    if (sharedContactStub.resolved())  sharedContactStub.get().getFooter().setVisibility(GONE);
    if (mediaThumbnailStub.resolved()) mediaThumbnailStub.require().getFooter().setVisibility(GONE);

    if (isFooterVisible(current, next, isGroupThread))
    {
      ConversationItemFooter activeFooter = getActiveFooter(current);
      activeFooter.setVisibility(VISIBLE);
      activeFooter.setMessageRecord(current, locale);

      if (hasWallpaper && hasNoBubble((messageRecord))) {
        if (messageRecord.isOutgoing()) {
          activeFooter.disableBubbleBackground();
          activeFooter.setTextColor(ContextCompat.getColor(context, R.color.conversation_item_sent_text_secondary_color));
          activeFooter.setIconColor(ContextCompat.getColor(context, R.color.conversation_item_sent_text_secondary_color));
          activeFooter.setRevealDotColor(ContextCompat.getColor(context, R.color.conversation_item_sent_text_secondary_color));
        } else {
          activeFooter.enableBubbleBackground(R.drawable.wallpaper_bubble_background_tintable_11,  getDefaultBubbleColor(hasWallpaper));
        }
      } else if (hasNoBubble(messageRecord)){
        activeFooter.disableBubbleBackground();
        activeFooter.setTextColor(ContextCompat.getColor(context, R.color.signal_text_secondary));
        activeFooter.setIconColor(ContextCompat.getColor(context, R.color.signal_icon_tint_secondary));
        activeFooter.setRevealDotColor(ContextCompat.getColor(context, R.color.signal_icon_tint_secondary));
      } else {
        activeFooter.disableBubbleBackground();
      }
    }
  }

  private boolean forceFooter(@NonNull MessageRecord messageRecord) {
    return hasAudio(messageRecord);
  }

  private ConversationItemFooter getActiveFooter(@NonNull MessageRecord messageRecord) {
    if (hasNoBubble(messageRecord) && stickerFooter != null) {
      return stickerFooter;
    } else if (hasSharedContact(messageRecord) && TextUtils.isEmpty(messageRecord.getDisplayBody(getContext()))) {
      return sharedContactStub.get().getFooter();
    } else if (hasOnlyThumbnail(messageRecord) && TextUtils.isEmpty(messageRecord.getDisplayBody(getContext()))) {
      return mediaThumbnailStub.require().getFooter();
    } else {
      return footer;
    }
  }

  private int readDimen(@DimenRes int dimenId) {
    return context.getResources().getDimensionPixelOffset(dimenId);
  }

  private boolean shouldInterceptClicks(MessageRecord messageRecord) {
    return batchSelected.isEmpty()                                                     &&
           ((messageRecord.isFailed() && !messageRecord.isMmsNotification())           ||
           (messageRecord.isRateLimited() && SignalStore.rateLimit().needsRecaptcha()) ||
           messageRecord.isPendingInsecureSmsFallback()                                ||
           messageRecord.isBundleKeyExchange());
  }

  @SuppressLint("SetTextI18n")
  private void setGroupMessageStatus(MessageRecord messageRecord, Recipient recipient) {
    if (groupThread && !messageRecord.isOutgoing() && groupSender != null) {
      groupSender.setText(recipient.getDisplayName(getContext()));
    }
  }

  private void setGroupAuthorColor(@NonNull MessageRecord messageRecord, boolean hasWallpaper, @NonNull Colorizer colorizer) {
    if (groupSender != null) {
        groupSender.setTextColor(colorizer.getIncomingGroupSenderColor(getContext(), messageRecord.getIndividualRecipient()));
    }
  }

  @SuppressWarnings("ConstantConditions")
  private void setAuthor(@NonNull MessageRecord current, @NonNull Optional<MessageRecord> previous, @NonNull Optional<MessageRecord> next, boolean isGroupThread, boolean hasWallpaper) {
    if (isGroupThread && !current.isOutgoing()) {
      contactPhotoHolder.setVisibility(VISIBLE);

      if (!previous.isPresent() || previous.get().isUpdate() || !current.getRecipient().equals(previous.get().getRecipient()) ||
          !DateUtils.isSameDay(previous.get().getTimestamp(), current.getTimestamp()))
      {
        groupSenderHolder.setVisibility(VISIBLE);

        if (hasWallpaper && hasNoBubble(current)) {
          groupSenderHolder.setBackgroundResource(R.drawable.wallpaper_bubble_background_tintable_11);
          groupSenderHolder.getBackground().setColorFilter(getDefaultBubbleColor(hasWallpaper), PorterDuff.Mode.MULTIPLY);
        } else {
          groupSenderHolder.setBackground(null);
        }
      } else {
        groupSenderHolder.setVisibility(GONE);
      }

      if (!next.isPresent() || next.get().isUpdate() || !current.getRecipient().equals(next.get().getRecipient())) {
        contactPhoto.setVisibility(VISIBLE);
      } else {
        contactPhoto.setVisibility(GONE);
      }
    } else {
      if (groupSenderHolder != null) {
        groupSenderHolder.setVisibility(GONE);
      }

      if (contactPhotoHolder != null) {
        contactPhotoHolder.setVisibility(GONE);
      }
    }
  }

  private void setOutlinerRadii(Outliner outliner, int topStart, int topEnd, int bottomEnd, int bottomStart) {
    if (ViewUtil.isRtl(this)) {
      outliner.setRadii(topEnd, topStart, bottomStart, bottomEnd);
    } else {
      outliner.setRadii(topStart, topEnd, bottomEnd, bottomStart);
    }
  }

  private @NonNull Projection.Corners getBodyBubbleCorners(int topStart, int topEnd, int bottomEnd, int bottomStart) {
    if (ViewUtil.isRtl(this)) {
      return new Projection.Corners(topEnd, topStart, bottomStart, bottomEnd);
    } else {
      return new Projection.Corners(topStart, topEnd, bottomEnd, bottomStart);
    }
  }

  private void setMessageShape(@NonNull MessageRecord current, @NonNull Optional<MessageRecord> previous, @NonNull Optional<MessageRecord> next, boolean isGroupThread) {
    int bigRadius   = readDimen(R.dimen.message_corner_radius);
    int smallRadius = readDimen(R.dimen.message_corner_collapse_radius);

    int background;

    if (isSingularMessage(current, previous, next, isGroupThread)) {
      if (current.isOutgoing()) {
        background = R.drawable.message_bubble_background_sent_alone;
        outliner.setRadius(bigRadius);
        pulseOutliner.setRadius(bigRadius);
        bodyBubbleCorners = new Projection.Corners(bigRadius);
      } else {
        background = R.drawable.message_bubble_background_received_alone;
        outliner.setRadius(bigRadius);
        pulseOutliner.setRadius(bigRadius);
        bodyBubbleCorners = null;
      }
    } else if (isStartOfMessageCluster(current, previous, isGroupThread)) {
      if (current.isOutgoing()) {
        background = R.drawable.message_bubble_background_sent_start;
        setOutlinerRadii(outliner, bigRadius, bigRadius, smallRadius, bigRadius);
        setOutlinerRadii(pulseOutliner, bigRadius, bigRadius, smallRadius, bigRadius);
        bodyBubbleCorners = getBodyBubbleCorners(bigRadius, bigRadius, smallRadius, bigRadius);
      } else {
        background = R.drawable.message_bubble_background_received_start;
        setOutlinerRadii(outliner, bigRadius, bigRadius, bigRadius, smallRadius);
        setOutlinerRadii(pulseOutliner, bigRadius, bigRadius, bigRadius, smallRadius);
        bodyBubbleCorners = null;
      }
    } else if (isEndOfMessageCluster(current, next, isGroupThread)) {
      if (current.isOutgoing()) {
        background = R.drawable.message_bubble_background_sent_end;
        setOutlinerRadii(outliner, bigRadius, smallRadius, bigRadius, bigRadius);
        setOutlinerRadii(pulseOutliner, bigRadius, smallRadius, bigRadius, bigRadius);
        bodyBubbleCorners = getBodyBubbleCorners(bigRadius, smallRadius, bigRadius, bigRadius);
      } else {
        background = R.drawable.message_bubble_background_received_end;
        setOutlinerRadii(outliner, smallRadius, bigRadius, bigRadius, bigRadius);
        setOutlinerRadii(pulseOutliner, smallRadius, bigRadius, bigRadius, bigRadius);
        bodyBubbleCorners = null;
      }
    } else {
      if (current.isOutgoing()) {
        background = R.drawable.message_bubble_background_sent_middle;
        setOutlinerRadii(outliner, bigRadius, smallRadius, smallRadius, bigRadius);
        setOutlinerRadii(pulseOutliner, bigRadius, smallRadius, smallRadius, bigRadius);
        bodyBubbleCorners = getBodyBubbleCorners(bigRadius, smallRadius, smallRadius, bigRadius);
      } else {
        background = R.drawable.message_bubble_background_received_middle;
        setOutlinerRadii(outliner, smallRadius, bigRadius, bigRadius, smallRadius);
        setOutlinerRadii(pulseOutliner, smallRadius, bigRadius, bigRadius, smallRadius);
        bodyBubbleCorners = null;
      }
    }

    bodyBubble.setBackgroundResource(background);
  }

  private boolean isStartOfMessageCluster(@NonNull MessageRecord current, @NonNull Optional<MessageRecord> previous, boolean isGroupThread) {
    if (isGroupThread) {
      return !previous.isPresent() || previous.get().isUpdate() || !DateUtils.isSameDay(current.getTimestamp(), previous.get().getTimestamp()) ||
             !current.getRecipient().equals(previous.get().getRecipient());
    } else {
      return !previous.isPresent() || previous.get().isUpdate() || !DateUtils.isSameDay(current.getTimestamp(), previous.get().getTimestamp()) ||
             current.isOutgoing() != previous.get().isOutgoing();
    }
  }

  private boolean isEndOfMessageCluster(@NonNull MessageRecord current, @NonNull Optional<MessageRecord> next, boolean isGroupThread) {
    if (isGroupThread) {
      return !next.isPresent() || next.get().isUpdate() || !DateUtils.isSameDay(current.getTimestamp(), next.get().getTimestamp()) ||
             !current.getRecipient().equals(next.get().getRecipient()) || !current.getReactions().isEmpty();
    } else {
      return !next.isPresent() || next.get().isUpdate() || !DateUtils.isSameDay(current.getTimestamp(), next.get().getTimestamp()) ||
             current.isOutgoing() != next.get().isOutgoing() || !current.getReactions().isEmpty();
    }
  }

  private boolean isSingularMessage(@NonNull MessageRecord current, @NonNull Optional<MessageRecord> previous, @NonNull Optional<MessageRecord> next, boolean isGroupThread) {
    return isStartOfMessageCluster(current, previous, isGroupThread) && isEndOfMessageCluster(current, next, isGroupThread);
  }

  private boolean isFooterVisible(@NonNull MessageRecord current, @NonNull Optional<MessageRecord> next, boolean isGroupThread) {
    boolean differentTimestamps = next.isPresent() && !DateUtils.isSameExtendedRelativeTimestamp(next.get().getTimestamp(), current.getTimestamp());

    return forceFooter(messageRecord) || current.getExpiresIn() > 0 || !current.isSecure() || current.isPending() || current.isPendingInsecureSmsFallback() ||
           current.isFailed() || current.isRateLimited() || differentTimestamps || isEndOfMessageCluster(current, next, isGroupThread);
  }

  private void setMessageSpacing(@NonNull Context context, @NonNull MessageRecord current, @NonNull Optional<MessageRecord> previous, @NonNull Optional<MessageRecord> next, boolean isGroupThread) {
    int spacingTop = readDimen(context, R.dimen.conversation_vertical_message_spacing_collapse);
    int spacingBottom = spacingTop;

    if (isStartOfMessageCluster(current, previous, isGroupThread)) {
      spacingTop = readDimen(context, R.dimen.conversation_vertical_message_spacing_default);
    }

    if (isEndOfMessageCluster(current, next, isGroupThread)) {
      spacingBottom = readDimen(context, R.dimen.conversation_vertical_message_spacing_default);
    }

    ViewUtil.setPaddingTop(this, spacingTop);
    ViewUtil.setPaddingBottom(this, spacingBottom);
  }

  private int readDimen(@NonNull Context context, @DimenRes int dimenId) {
    return context.getResources().getDimensionPixelOffset(dimenId);
  }

  /// Event handlers

  private Spannable getLongMessageSpan(@NonNull MessageRecord messageRecord) {
    String   message;
    Runnable action;

    if (messageRecord.isMms()) {
      TextSlide slide = ((MmsMessageRecord) messageRecord).getSlideDeck().getTextSlide();

      if (slide != null && slide.asAttachment().getTransferState() == AttachmentDatabase.TRANSFER_PROGRESS_DONE) {
        message = getResources().getString(R.string.ConversationItem_read_more);
        action  = () -> eventListener.onMoreTextClicked(conversationRecipient.getId(), messageRecord.getId(), messageRecord.isMms());
      } else if (slide != null && slide.asAttachment().getTransferState() == AttachmentDatabase.TRANSFER_PROGRESS_STARTED) {
        message = getResources().getString(R.string.ConversationItem_pending);
        action  = () -> {};
      } else if (slide != null) {
        message = getResources().getString(R.string.ConversationItem_download_more);
        action  = () -> singleDownloadClickListener.onClick(bodyText, slide);
      } else {
        message = getResources().getString(R.string.ConversationItem_read_more);
        action  = () -> eventListener.onMoreTextClicked(conversationRecipient.getId(), messageRecord.getId(), messageRecord.isMms());
      }
    } else {
      message = getResources().getString(R.string.ConversationItem_read_more);
      action  = () -> eventListener.onMoreTextClicked(conversationRecipient.getId(), messageRecord.getId(), messageRecord.isMms());
    }

    SpannableStringBuilder span = new SpannableStringBuilder(message);
    CharacterStyle style = new ClickableSpan() {
      @Override
      public void onClick(@NonNull View widget) {
        if (eventListener != null && batchSelected.isEmpty()) {
          action.run();
        }
      }

      @Override
      public void updateDrawState(@NonNull TextPaint ds) {
        ds.setTypeface(Typeface.DEFAULT_BOLD);
      }
    };
    span.setSpan(style, 0, span.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
    return span;
  }

  @Override
  public void showProjectionArea() {
    if (mediaThumbnailStub != null && mediaThumbnailStub.resolved()) {
      mediaThumbnailStub.require().showThumbnailView();
      bodyBubble.setVideoPlayerProjection(null);
    }
  }

  @Override
  public void hideProjectionArea() {
    if (mediaThumbnailStub != null && mediaThumbnailStub.resolved()) {
      mediaThumbnailStub.require().hideThumbnailView();
      mediaThumbnailStub.require().getDrawingRect(thumbnailMaskingRect);
      bodyBubble.setVideoPlayerProjection(Projection.relativeToViewWithCommonRoot(mediaThumbnailStub.require(), bodyBubble, null));
    }
  }

  @Override
  public @Nullable MediaItem getMediaItem() {
    return mediaItem;
  }

  @Override
  public @Nullable GiphyMp4PlaybackPolicyEnforcer getPlaybackPolicyEnforcer() {
    if (GiphyMp4PlaybackPolicy.autoplay()) {
      return null;
    } else {
      return new GiphyMp4PlaybackPolicyEnforcer(() -> {
        eventListener.onPlayInlineContent(null);
      });
    }
  }

  @Override
  public int getAdapterPosition() {
    throw new UnsupportedOperationException("Do not delegate to this method");
  }

  @Override
  public @NonNull Projection getGiphyMp4PlayableProjection(@NonNull ViewGroup recyclerView) {
    if (mediaThumbnailStub != null && mediaThumbnailStub.isResolvable()) {
      return Projection.relativeToParent(recyclerView, mediaThumbnailStub.require(), mediaThumbnailStub.require().getCorners())
                       .translateX(bodyBubble.getTranslationX());
    } else {
      return Projection.relativeToParent(recyclerView, bodyBubble, bodyBubbleCorners)
                       .translateX(bodyBubble.getTranslationX());
    }
  }

  @Override
  public boolean canPlayContent() {
    return mediaThumbnailStub != null && mediaThumbnailStub.isResolvable() && canPlayContent;
  }

  @Override
  public @NonNull List<Projection> getColorizerProjections() {
    List<Projection> projections = new LinkedList<>();

    if (messageRecord.isOutgoing()      &&
        !hasNoBubble(messageRecord)     &&
        !messageRecord.isRemoteDelete() &&
        bodyBubbleCorners != null)
    {
      projections.add(Projection.relativeToViewRoot(bodyBubble, bodyBubbleCorners).translateX(bodyBubble.getTranslationX()));
    }

    if (messageRecord.isOutgoing() &&
        hasNoBubble(messageRecord) &&
        hasWallpaper)
    {
      Projection footerProjection = getActiveFooter(messageRecord).getProjection();
      if (footerProjection != null) {
        projections.add(footerProjection.translateX(bodyBubble.getTranslationX()));
      }
    }

    if (!messageRecord.isOutgoing() &&
        hasQuote(messageRecord)     &&
        quoteView != null)
    {
      bodyBubble.setQuoteViewProjection(quoteView.getProjection(bodyBubble));
      projections.add(quoteView.getProjection((ViewGroup) getRootView()).translateX(bodyBubble.getTranslationX() + this.getTranslationX()));
    }

    return projections;
  }

  @Override
  public @Nullable View getHorizontalTranslationTarget() {
    if (messageRecord.isOutgoing()) {
      return null;
    } else if (groupThread) {
      return contactPhotoHolder;
    } else {
      return bodyBubble;
    }
  }

  private class SharedContactEventListener implements SharedContactView.EventListener {
    @Override
    public void onAddToContactsClicked(@NonNull Contact contact) {
      if (eventListener != null && batchSelected.isEmpty()) {
        eventListener.onAddToContactsClicked(contact);
      } else {
        passthroughClickListener.onClick(sharedContactStub.get());
      }
    }

    @Override
    public void onInviteClicked(@NonNull List<Recipient> choices) {
      if (eventListener != null && batchSelected.isEmpty()) {
        eventListener.onInviteSharedContactClicked(choices);
      } else {
        passthroughClickListener.onClick(sharedContactStub.get());
      }
    }

    @Override
    public void onMessageClicked(@NonNull List<Recipient> choices) {
      if (eventListener != null && batchSelected.isEmpty()) {
        eventListener.onMessageSharedContactClicked(choices);
      } else {
        passthroughClickListener.onClick(sharedContactStub.get());
      }
    }
  }

  private class SharedContactClickListener implements View.OnClickListener {
    @Override
    public void onClick(View view) {
      if (eventListener != null && batchSelected.isEmpty() && messageRecord.isMms() && !((MmsMessageRecord) messageRecord).getSharedContacts().isEmpty()) {
        eventListener.onSharedContactDetailsClicked(((MmsMessageRecord) messageRecord).getSharedContacts().get(0), sharedContactStub.get().getAvatarView());
      } else {
        passthroughClickListener.onClick(view);
      }
    }
  }

  private class LinkPreviewClickListener implements View.OnClickListener {
    @Override
    public void onClick(View view) {
      if (eventListener != null && batchSelected.isEmpty() && messageRecord.isMms() && !((MmsMessageRecord) messageRecord).getLinkPreviews().isEmpty()) {
        eventListener.onLinkPreviewClicked(((MmsMessageRecord) messageRecord).getLinkPreviews().get(0));
      } else {
        passthroughClickListener.onClick(view);
      }
    }
  }

  private class ViewOnceMessageClickListener implements View.OnClickListener {
    @Override
    public void onClick(View view) {
      ViewOnceMessageView revealView = (ViewOnceMessageView) view;

      if (batchSelected.isEmpty() && messageRecord.isMms() && revealView.requiresTapToDownload((MmsMessageRecord) messageRecord)) {
        singleDownloadClickListener.onClick(view, ((MmsMessageRecord) messageRecord).getSlideDeck().getThumbnailSlide());
      } else if (eventListener != null && batchSelected.isEmpty() && messageRecord.isMms()) {
        eventListener.onViewOnceMessageClicked((MmsMessageRecord) messageRecord);
      } else {
        passthroughClickListener.onClick(view);
      }
    }
  }

  private class LinkPreviewThumbnailClickListener implements SlideClickListener {
    public void onClick(final View v, final Slide slide) {
      if (eventListener != null && batchSelected.isEmpty() && messageRecord.isMms() && !((MmsMessageRecord) messageRecord).getLinkPreviews().isEmpty()) {
        eventListener.onLinkPreviewClicked(((MmsMessageRecord) messageRecord).getLinkPreviews().get(0));
      } else {
        performClick();
      }
    }
  }

  private class AttachmentDownloadClickListener implements SlidesClickedListener {
    @Override
    public void onClick(View v, final List<Slide> slides) {
      Log.i(TAG, "onClick() for attachment download");
      if (messageRecord.isMmsNotification()) {
        Log.i(TAG, "Scheduling MMS attachment download");
        ApplicationDependencies.getJobManager().add(new MmsDownloadJob(messageRecord.getId(),
                                                                       messageRecord.getThreadId(),
                                                                       false));
      } else {
        Log.i(TAG, "Scheduling push attachment downloads for " + slides.size() + " items");

        for (Slide slide : slides) {
          ApplicationDependencies.getJobManager().add(new AttachmentDownloadJob(messageRecord.getId(),
                                                                                ((DatabaseAttachment)slide.asAttachment()).getAttachmentId(),
                                                                                true));
        }
      }
    }
  }

  private class SlideClickPassthroughListener implements SlideClickListener {

    private final SlidesClickedListener original;

    private SlideClickPassthroughListener(@NonNull SlidesClickedListener original) {
      this.original = original;
    }

    @Override
    public void onClick(View v, Slide slide) {
      original.onClick(v, Collections.singletonList(slide));
    }
  }

  private class StickerClickListener implements SlideClickListener {
    @Override
    public void onClick(View v, Slide slide) {
      if (shouldInterceptClicks(messageRecord) || !batchSelected.isEmpty()) {
        performClick();
      } else if (eventListener != null && hasSticker(messageRecord)) {
        //noinspection ConstantConditions
        eventListener.onStickerClicked(((MmsMessageRecord) messageRecord).getSlideDeck().getStickerSlide().asAttachment().getSticker());
      }
    }
  }

  private class ThumbnailClickListener implements SlideClickListener {
    public void onClick(final View v, final Slide slide) {
      if (shouldInterceptClicks(messageRecord) || !batchSelected.isEmpty()) {
        performClick();
      } else if (!canPlayContent && mediaItem != null && eventListener != null) {
        eventListener.onPlayInlineContent(conversationMessage);
      } else if (MediaPreviewActivity.isContentTypeSupported(slide.getContentType()) && slide.getUri() != null) {
        Intent intent = new Intent(context, MediaPreviewActivity.class);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setDataAndType(slide.getUri(), slide.getContentType());
        intent.putExtra(MediaPreviewActivity.THREAD_ID_EXTRA, messageRecord.getThreadId());
        intent.putExtra(MediaPreviewActivity.DATE_EXTRA, messageRecord.getTimestamp());
        intent.putExtra(MediaPreviewActivity.SIZE_EXTRA, slide.asAttachment().getSize());
        intent.putExtra(MediaPreviewActivity.CAPTION_EXTRA, slide.getCaption().orNull());
        intent.putExtra(MediaPreviewActivity.IS_VIDEO_GIF, slide.isVideoGif());
        intent.putExtra(MediaPreviewActivity.LEFT_IS_RECENT_EXTRA, false);

        context.startActivity(intent);
      } else if (slide.getUri() != null) {
        Log.i(TAG, "Clicked: " + slide.getUri() + " , " + slide.getContentType());
        Uri publicUri = PartAuthority.getAttachmentPublicUri(slide.getUri());
        Log.i(TAG, "Public URI: " + publicUri);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setDataAndType(PartAuthority.getAttachmentPublicUri(slide.getUri()), Intent.normalizeMimeType(slide.getContentType()));
        try {
          context.startActivity(intent);
        } catch (ActivityNotFoundException anfe) {
          Log.w(TAG, "No activity existed to view the media.");
          Toast.makeText(context, R.string.ConversationItem_unable_to_open_media, Toast.LENGTH_LONG).show();
        }
      }
    }
  }

  private class PassthroughClickListener implements View.OnLongClickListener, View.OnClickListener {

    @Override
    public boolean onLongClick(View v) {
      if (bodyText.hasSelection()) {
        return false;
      }
      performLongClick();
      return true;
    }

    @Override
    public void onClick(View v) {
      performClick();
    }
  }

  private class ClickListener implements View.OnClickListener {
    private OnClickListener parent;

    ClickListener(@Nullable OnClickListener parent) {
      this.parent = parent;
    }

    public void onClick(View v) {
      if (!shouldInterceptClicks(messageRecord) && parent != null) {
        parent.onClick(v);
      } else if (messageRecord.isFailed()) {
        if (eventListener != null) {
          eventListener.onMessageWithErrorClicked(messageRecord);
        }
      } else if (messageRecord.isRateLimited() && SignalStore.rateLimit().needsRecaptcha()) {
        if (eventListener != null) {
          eventListener.onMessageWithRecaptchaNeededClicked(messageRecord);
        }
      } else if (!messageRecord.isOutgoing() && messageRecord.isIdentityMismatchFailure()) {
        if (eventListener != null) {
          eventListener.onIncomingIdentityMismatchClicked(messageRecord.getIndividualRecipient().getId());
        }
      } else if (messageRecord.isPendingInsecureSmsFallback()) {
        handleMessageApproval();
      }
    }
  }

  private final class TouchDelegateChangedListener implements ConversationItemFooter.OnTouchDelegateChangedListener {
    @Override
    public void onTouchDelegateChanged(@NonNull Rect delegateRect, @NonNull View delegateView) {
      offsetDescendantRectToMyCoords(footer, delegateRect);
      setTouchDelegate(new TouchDelegate(delegateRect, delegateView));
    }
  }

  private final class UrlClickListener implements UrlClickHandler {

    @Override
    public boolean handleOnClick(@NonNull String url) {
      return eventListener != null && eventListener.onUrlClicked(url);
    }
  }

  private class MentionClickableSpan extends ClickableSpan {
    private final RecipientId mentionedRecipientId;

    MentionClickableSpan(RecipientId mentionedRecipientId) {
      this.mentionedRecipientId = mentionedRecipientId;
    }

    @Override
    public void onClick(@NonNull View widget) {
      if (eventListener != null && batchSelected.isEmpty()) {
        VibrateUtil.vibrateTick(context);
        eventListener.onGroupMemberClicked(mentionedRecipientId, conversationRecipient.get().requireGroupId());
      }
    }

    @Override
    public void updateDrawState(@NonNull TextPaint ds) { }
  }

  private final class AudioPlaybackSpeedToggleListener implements PlaybackSpeedToggleTextView.PlaybackSpeedListener {
    @Override
    public void onPlaybackSpeedChanged(float speed) {
      if (eventListener == null || !audioViewStub.resolved()) {
        return;
      }

      Uri uri = audioViewStub.get().getAudioSlideUri();
      if (uri == null) {
        return;
      }

      eventListener.onVoiceNotePlaybackSpeedChanged(uri, speed);
    }
  }

  private final class AudioViewCallbacks implements AudioView.Callbacks {

    @Override
    public void onPlay(@NonNull Uri audioUri, double progress) {
      if (eventListener == null) return;

      eventListener.onVoiceNotePlay(audioUri, messageRecord.getId(), progress);
    }

    @Override
    public void onPause(@NonNull Uri audioUri) {
      if (eventListener == null) return;

      eventListener.onVoiceNotePause(audioUri);
    }

    @Override
    public void onSeekTo(@NonNull Uri audioUri, double progress) {
      if (eventListener == null) return;

      eventListener.onVoiceNoteSeekTo(audioUri, progress);
    }

    @Override
    public void onStopAndReset(@NonNull Uri audioUri) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void onSpeedChanged(float speed, boolean isPlaying) {
      footer.setAudioPlaybackSpeed(speed, isPlaying);
    }

    @Override
    public void onProgressUpdated(long durationMillis, long playheadMillis) {
      footer.setAudioDuration(durationMillis, playheadMillis);
    }
  }

  private void handleMessageApproval() {
    final int title;
    final int message;

    if (messageRecord.isMms()) title = R.string.ConversationItem_click_to_approve_unencrypted_mms_dialog_title;
    else                       title = R.string.ConversationItem_click_to_approve_unencrypted_sms_dialog_title;

    message = R.string.ConversationItem_click_to_approve_unencrypted_dialog_message;

    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder.setTitle(title);

    if (message > -1) builder.setMessage(message);

    builder.setPositiveButton(R.string.yes, (dialogInterface, i) -> {
      MessageDatabase db = messageRecord.isMms() ? DatabaseFactory.getMmsDatabase(context)
                                                 : DatabaseFactory.getSmsDatabase(context);

      db.markAsInsecure(messageRecord.getId());
      db.markAsOutbox(messageRecord.getId());
      db.markAsForcedSms(messageRecord.getId());

      if (messageRecord.isMms()) {
        MmsSendJob.enqueue(context,
                           ApplicationDependencies.getJobManager(),
                           messageRecord.getId());
      } else {
        ApplicationDependencies.getJobManager().add(new SmsSendJob(messageRecord.getId(),
                                                                   messageRecord.getIndividualRecipient()));
      }
    });

    builder.setNegativeButton(R.string.no, (dialogInterface, i) -> {
      if (messageRecord.isMms()) {
        DatabaseFactory.getMmsDatabase(context).markAsSentFailed(messageRecord.getId());
      } else {
        DatabaseFactory.getSmsDatabase(context).markAsSentFailed(messageRecord.getId());
      }
    });
    builder.show();
  }
}
