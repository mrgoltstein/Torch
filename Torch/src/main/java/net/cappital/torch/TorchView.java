/*
 * Copyright 2013 Reinier Goltstein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.cappital.torch;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorInflater;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.ValueAnimator;
import com.nineoldandroids.view.ViewHelper;
import com.nineoldandroids.view.ViewPropertyAnimator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * In-layout notification library with support for multiple concurrently displayed Toast-like
 * messages or consecutively displayed in-layout notifications with support for swipe-to-dismiss and
 * drawables/buttons.
 * <p/>
 * <B>Usage:</B> <BR/> To provide your own custom View, call {@link #setViewBuilder(ViewBuilder)}}
 * or extend this class and override {@link #getView(Message)}.
 */
public class TorchView extends LinearLayout {

	/**
	 * Interface that allows you to supply a custom View to {@link net.cappital.torch.TorchView}.
	 */
	public static interface ViewBuilder {
		public View getView(Context context, Message message);
	}

	private static boolean DEBUG = false;

	private static final String TAG = "TorchView";

	@SuppressWarnings("unused")
	public static final int LENGTH_SHORT = 2000;

	@SuppressWarnings("unused")
	public static final int LENGTH_LONG = 5000;

	private boolean isAnimating = false;

	private long resizeAnimationDuration = DEBUG ? 600 : 300;

	private long moveAnimationDuration = DEBUG ? 1000 : 300;

	private long displayAnimationDuration = DEBUG ? 600 : 300;

	protected int messageDuration = Toast.LENGTH_SHORT;

	/**
	 * This flag defines the interval between delete-operations. Messages are deleted only once
	 * every few milliseconds as defined by this flag to assure the user is able to see what changes
	 * are happening to the View.
	 */
	protected long delayBetweenMessages = 300;

	/**
	 * Flag that indicates the maximum number of messages to be shown at any time.
	 */
	protected int maxConcurrent = 1;

	/**
	 * Flag that indicates whether a remove-operation is currently taking place. When the value of
	 * this flag is {@value true}, operations in {@link #readNext()}} are paused.
	 */
	private boolean removingMessage = false;

	/**
	 * In-animation resource for the container-View. This is used for every 1st message to be shown.
	 * Otherwise, the {@link net.cappital.torch.Message Message's} {@link
	 * Message#inAnimationResource} is used.
	 */
	protected int inAnimationResource = R.anim.torch_wrapper_in_default;

	/**
	 * Out-animation resource for the container-View. This is used for every last message to be
	 * shown. Otherwise, the {@link net.cappital.torch.Message Message's} {@link
	 * Message#outAnimationResource} is used.
	 */
	protected int outAnimationResource = R.anim.torch_wrapper_out_default;

	/**
	 * Optional reference to a class that provides a custom {@link View} for {@link Message
	 * Messages}.
	 * <p/>
	 * To provide your own custom Views for Messages, call {@link #setViewBuilder(net.cappital.torch.TorchView.ViewBuilder)}
	 * or override {@link TorchView#getView(Message)}.
	 */
	private ViewBuilder viewBuilder;

	private HashMap<Message, View> mViews = new HashMap<Message, View>();

	private List<Message> messageQueue = Collections.synchronizedList(new LinkedList<Message>());

	private LinkedBlockingQueue<Message> removalQueue = new LinkedBlockingQueue<Message>();

	private ArrayList<Message> currentMessages = new ArrayList<Message>();

	public TorchView(Context context, int concurrentMessages, int messageDuration) {
		super(context);

		this.maxConcurrent = concurrentMessages;
		this.messageDuration = messageDuration;
		this.delayBetweenMessages = 300;

		init(context);
	}

	private void init(Context context) {
		messageQueue = new ArrayList<Message>();
		currentMessages = new ArrayList<Message>();
		removalQueue = new LinkedBlockingQueue<Message>();

		Resources r = context.getResources();

		if (this.viewBuilder == null && getContext() instanceof ViewBuilder) {
			this.viewBuilder = (ViewBuilder) getContext();
		}

		setClipChildren(false);
		setOrientation(LinearLayout.VERTICAL);
		setVisibility(View.GONE);
		setGravity(Gravity.BOTTOM); // TODO adjust gravity for addMessageView-to-top/bottom
		setPadding(r.getDimensionPixelSize(R.dimen.torchview_padding_left),
				r.getDimensionPixelSize(R.dimen.torchview_padding_top),
				r.getDimensionPixelSize(R.dimen.torchview_padding_right),
				r.getDimensionPixelSize(R.dimen.torchview_padding_bottom));
		setInAnimationResource(inAnimationResource);
		setOutAnimationResource(outAnimationResource);

		FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
				FrameLayout.LayoutParams.WRAP_CONTENT);
		lp.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
		lp.rightMargin = r.getDimensionPixelSize(R.dimen.torchview_margin_right);
		lp.bottomMargin = r.getDimensionPixelSize(R.dimen.torchview_margin_bottom);
		lp.leftMargin = r.getDimensionPixelSize(R.dimen.torchview_margin_left);
		setLayoutParams(lp);
	}

	public void attach(Activity activity) {
		if (getParent() != null && getParent() instanceof ViewGroup) {
			((ViewGroup) getParent()).removeView(this);
		}
		ViewGroup rootView = (ViewGroup) activity.findViewById(android.R.id.content);
		rootView.addView(this);
	}

	public void setViewBuilder(ViewBuilder viewBuilder) {
		this.viewBuilder = viewBuilder;
	}

	private void resizeContainer(final Animator.AnimatorListener animatorListener, Animator... additionalAnimators) {
		final ArrayList<Animator> animators = new ArrayList<Animator>(Arrays.asList(additionalAnimators));
		final int fromHeight = getHeight();

		final ViewTreeObserver viewTreeObserver = getViewTreeObserver();
		if (viewTreeObserver != null) {
			viewTreeObserver.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
				@Override
				public boolean onPreDraw() {
					viewTreeObserver.removeOnPreDrawListener(this);

					// expand boundaries gracefully
					int toHeight = getHeight();
					animators.add(getHeightAnimator(fromHeight, toHeight, null));

					AnimatorSet set = new AnimatorSet();
					set.setDuration(resizeAnimationDuration);
					set.addListener(animatorListener);
					set.playTogether(animators);
					set.start();

					return false;
				}
			});
		}
	}

	private ValueAnimator getHeightAnimator(int fromHeight, final int toHeight, Animator.AnimatorListener listener) {
		ValueAnimator heightAnim = ValueAnimator.ofInt(fromHeight, toHeight);
		heightAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator valueAnimator) {
				int val = (Integer) valueAnimator.getAnimatedValue();
				ViewGroup.LayoutParams layoutParams = getLayoutParams();
				if (val == toHeight) {
					val = ViewGroup.LayoutParams.WRAP_CONTENT;
				}
				if (layoutParams != null) {
					layoutParams.height = val;
					setLayoutParams(layoutParams);
				}
			}
		});

		if (listener != null) {
			heightAnim.addListener(listener);
		}

		return heightAnim;
	}

	private void onMessageDisplayed(final Message message) {
		// -> child animation done, start timer
		if (messageDuration > 0) {
			// start timer for removing
			postDelayed(new Runnable() {
				@Override
				public void run() {
					removeMessage(message);
				}
			}, messageDuration);
		}

		// see if we can addMessageView more messages
		readQueue();
	}

	private View getMessageView(Message message) {
		View view = null;
		if (this.viewBuilder != null) {
			view = viewBuilder.getView(getContext(), message);
		}

		if (view == null) {
			view = getView(message);
		}

		final View mView = view;
		mView.setClickable(true);
		mView.setOnTouchListener(new SwipeDismissTouchListener(mView, message, new SwipeDismissTouchListener.DismissCallbacks() {
			@Override
			public boolean canDismiss(Object token) {
				return ((Message) token).isDismissable();
			}

			@Override
			public void onDismiss(View view, Object token) {
				Message message = (Message) token;
				dismissView(message);
			}
		}));

		return mView;
	}

	/**
	 * Returns the default View for {@link net.cappital.torch.Message}. You can override this method
	 * for your own custom implementation.
	 *
	 * @param message The {@link net.cappital.torch.Message} to show.
	 *
	 * @return The {@link View} to show for {@link net.cappital.torch.Message}.
	 */
	public View getView(Message message) {
		Context context = getContext();
		View v = null;

		if (context != null) {
			v = View.inflate(context, R.layout.default_message, null);
			TextView tv = (TextView) v.findViewById(R.id.torch_message);
			if (tv != null) {
				tv.setText(message.getContent());
			}
		}

		return v;
	}

	private void onMessageRemoved(Message message) {
		// if we dismissed item, remove it from removalQueue
		removalQueue.remove(message);
		currentMessages.remove(message);
		readQueue();
	}

	private void showContainer(final Message initialMessage, Animator... animators) {
		ArrayList<Animator> additionalAnimators = new ArrayList<Animator>(Arrays.asList(animators));

		setVisibility(View.VISIBLE);

		Animator showAnimator = AnimatorInflater.loadAnimator(getContext(), inAnimationResource);
		showAnimator.setTarget(this);
		if (showAnimator.getDuration() > 0) {
			displayAnimationDuration = showAnimator.getDuration();
		} else {
			showAnimator.setDuration(displayAnimationDuration);
		}

		additionalAnimators.add(showAnimator);

		AnimatorSet set = new AnimatorSet();
		set.setDuration(displayAnimationDuration);
		set.playTogether(additionalAnimators);
		set.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationStart(Animator animation) {
				isAnimating = true;
			}

			@Override
			public void onAnimationEnd(Animator animation) {
				isAnimating = false;

				// start timer for initial message
				onMessageDisplayed(initialMessage);
			}
		});

		set.start();
	}

	private void hideContainer(final Message... messages) {
		Animator animator = AnimatorInflater.loadAnimator(getContext(), outAnimationResource);
		animator.setTarget(this);
		if (animator.getDuration() <= 0) {
			animator.setDuration(displayAnimationDuration);
		}

		animator.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationStart(Animator animation) {
				isAnimating = true;
			}

			@Override
			public void onAnimationEnd(Animator animation) {
				isAnimating = false;
				setVisibility(View.GONE);
				removeAllViews();

				// update state of final message
				for (Message message : messages) {
					onMessageRemoved(message);
				}
			}

			@Override
			public void onAnimationCancel(Animator animation) {
				isAnimating = false;
			}
		});
		animator.start();
	}

	public void clear(boolean interrupt) {

		this.messageQueue.clear();

		if (interrupt) {
			currentMessages.clear();
			removalQueue.clear();

			for (View v : mViews.values()) {
				// cancel running animations
				ViewPropertyAnimator.animate(v).cancel();
			}
			mViews.clear();
			removeAllViews();
		}
	}

	public void setInAnimationResource(int inAnimationResource) {
		this.inAnimationResource = inAnimationResource;
	}

	public void setOutAnimationResource(int outAnimationResource) {
		this.outAnimationResource = outAnimationResource;
	}

	public void show(Message message) {
		add(message);
		show();
	}

	public void show(ArrayList<Message> messages) {
		for (Message msg : messages) {
			add(msg);
		}
		show();
	}

	public void show() {
		readQueue();
	}

	/**
	 * Adds item to queue.
	 *
	 * @param message {@link Message} to display.
	 */
	private void add(Message message) {
		// skip message with ID's if duplicate is found in currently displayed messages or queue
		if (message.getId() > 0) {
			boolean itemFound = findMessage(message, currentMessages)
					|| findMessage(message, messageQueue);

			if (itemFound) {
				// skip item: already in queue
				return;
			}
		}

		messageQueue.add(message);
	}

	private void dismissView(final Message message) {
		View mView = mViews.remove(message);

		if (mViews.size() == 0) {
			hideContainer(message);
		}

		if (mView != null) {
			removeView(mView);
			onMessageRemoved(message);
		}
	}

	/**
	 * Adds message to the queue for items to be removed.
	 *
	 * @param message The {@link net.cappital.torch.Message} to be removed.
	 */
	private void removeMessage(Message message) {
		if (currentMessages.contains(message)) {
			removalQueue.offer(message);
			readQueue();
		}
	}

	/**
	 * Clears the flag {@link #removingMessage} after a period defined by {@link
	 * #delayBetweenMessages}, which allows {@link #readNext()} to perform the next operation.
	 * <p/>
	 * Messages are deleted only once every few milliseconds as defined by {@link
	 * #delayBetweenMessages}. This is done to assure the user is able to see what changes are
	 * happening to the View.
	 */
	private void clearRemoveStatus() {
		postDelayed(new Runnable() {
			@Override
			public void run() {
				removingMessage = false;
				readQueue();
			}
		}, delayBetweenMessages);
	}

	private void readQueue() {
		// post because want to finish previous execution before continuing
		post(new Runnable() {
			@Override
			public void run() {
				readNext();
			}
		});
	}

	/**
	 * Reads the queue and performs required actions: add/remove/replace Views.
	 */
	private void readNext() {
		// check if state is OK
		if (getParent() == null || !(getParent() instanceof ViewGroup)) {
			throw new RuntimeException("TorchView has no parent View");
		}

		if (!removingMessage && !isAnimating) {
			Message removeMessage = removalQueue.poll();
			if (removeMessage != null) {
				removingMessage = true;

				Message addMessage = messageQueue.size() > 0 ? messageQueue.remove(0) : null;
				if (addMessage == null) {
					removeMessageView(removeMessage);
				} else {
					currentMessages.add(addMessage);
					replaceMessageView(removeMessage, addMessage);
				}
				clearRemoveStatus();

			} else if (maxConcurrent > currentMessages.size()) {
				// more space available: move message from queue to currentMessages
				Message addMessage = messageQueue.size() > 0 ? messageQueue.remove(0) : null;
				if (addMessage != null) {
					currentMessages.add(addMessage);
					addMessageView(addMessage);
				}
			}// else ignore; readQueue() will be called again when space is available
		}
	}

	private void addMessageView(final Message message) {
		final View messageView = getMessageView(message);
		addView(messageView);
		mViews.put(message, messageView);

		final Animator addAnimation = AnimatorInflater.loadAnimator(getContext(), message.getInAnimationResource());
		addAnimation.setTarget(messageView);

		if (getChildCount() == 1) {
			// show wrapper & notify manager when done
			showContainer(message);

		} else {
			addAnimation.setDuration(displayAnimationDuration);
			resizeContainer(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationStart(Animator animation) {
					isAnimating = true;
				}

				@Override
				public void onAnimationEnd(Animator animation) {
					isAnimating = false;
					onMessageDisplayed(message);
				}

				@Override
				public void onAnimationCancel(Animator animation) {
					isAnimating = false;
				}
			}, addAnimation);
		}
	}

	private void removeMessageView(final Message message) {
		if (getChildCount() <= 1) {
			// down to last message, which is now expired.. hide wrapper
			hideContainer(message);

		} else {
			final View view = mViews.remove(message);
			if (view != null) {
				// oldMessage -> out-animation
				Animator removeAnimation = AnimatorInflater.loadAnimator(getContext(), message.getOutAnimationResource());
				removeAnimation.setTarget(view);
				removeAnimation.setDuration(moveAnimationDuration);

				removeAnimation.addListener(new AnimatorListenerAdapter() {
					@Override
					public void onAnimationStart(Animator animation) {
						isAnimating = true;
					}

					@Override
					public void onAnimationEnd(Animator animation) {
						// remove View and resize container
						removeView(view);

						resizeContainer(new AnimatorListenerAdapter() {
							@Override
							public void onAnimationEnd(Animator animation) {
								isAnimating = false;
								onMessageRemoved(message);
							}
						});
					}
				});

				removeAnimation.start();
			}
		}
	}

	private void replaceMessageView(final Message oldMessage, final Message newMessage) {

		Log.d(TAG, "replaceMessageView()");

		final View oldView = mViews.remove(oldMessage);
		if (oldView == null) {
			addMessageView(newMessage);
		} else {
			Log.d(TAG, "oldView != null");
			ArrayList<Animator> animators = new ArrayList<Animator>();

			// oldMessage -> out-animation
			Animator messageAnim = AnimatorInflater.loadAnimator(getContext(), oldMessage.getOutAnimationResource());
			messageAnim.setTarget(oldView);
			animators.add(messageAnim);

			// other messages -> animate up
			Animator moveAnimation;
			for (View v : mViews.values()) {
				moveAnimation = ObjectAnimator.ofFloat(v, "translationY", 0f, -oldView.getHeight());
				animators.add(moveAnimation);
			}

			final View newMessageView = getMessageView(newMessage);
			mViews.put(newMessage, newMessageView);

			AnimatorSet set = new AnimatorSet();
			set.playTogether(animators);
			set.setDuration(moveAnimationDuration);
			set.addListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationStart(Animator animation) {
					isAnimating = true;
				}

				@Override
				public void onAnimationEnd(Animator animation) {
					removeView(oldView);
					onMessageRemoved(oldMessage);

					for (View v : mViews.values()) {
						ViewHelper.setTranslationY(v, 0);
					}

					addView(newMessageView);
					Animator addAnimation = AnimatorInflater.loadAnimator(getContext(), newMessage.getInAnimationResource());
					addAnimation.setTarget(newMessageView);
					addAnimation.setDuration(resizeAnimationDuration);
					addAnimation.addListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							isAnimating = false;
							onMessageDisplayed(newMessage);
						}
					});
					addAnimation.start();
				}
			});
			set.start();
		}
	}

	private boolean findMessage(Message message, Collection<Message> collection) {
		Collection<Message> messages = Collections.synchronizedCollection(collection);
		for (Message m : messages) {
			if (message.getId() == m.getId()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Parcelable onSaveInstanceState() {
		Parcelable superState = super.onSaveInstanceState();

		SavedState ss = new SavedState(superState);

		ss.delayBetweenMessages = delayBetweenMessages;
		ss.inAnimationResource = inAnimationResource;
		ss.maxConcurrent = maxConcurrent;
		ss.messageDuration = messageDuration;
		ss.outAnimationResource = outAnimationResource;

		ArrayList<Message> messages = new ArrayList<Message>();
		messages.addAll(currentMessages);
		messages.addAll(messageQueue);
		ss.messages = messages;

		return ss;
	}

	@Override
	public void onRestoreInstanceState(Parcelable state) {
		if (!(state instanceof SavedState)) {
			super.onRestoreInstanceState(state);
			return;
		}

		SavedState ss = (SavedState) state;
		super.onRestoreInstanceState(ss.getSuperState());

		delayBetweenMessages = ss.delayBetweenMessages;
		inAnimationResource = ss.inAnimationResource;
		maxConcurrent = ss.maxConcurrent;
		messageDuration = ss.messageDuration;
		outAnimationResource = ss.outAnimationResource;

		currentMessages = new ArrayList<Message>();
		removalQueue = new LinkedBlockingQueue<Message>();
		messageQueue = new ArrayList<Message>();
		messageQueue.addAll(ss.messages);
	}

	static class SavedState extends BaseSavedState {

		private long delayBetweenMessages;
		private int maxConcurrent;
		private int messageDuration;
		private int inAnimationResource;
		private int outAnimationResource;
		private ArrayList<Message> messages;

		SavedState(Parcelable superState) {
			super(superState);
		}

		private SavedState(Parcel in) {
			super(in);

			this.delayBetweenMessages = in.readLong();
			this.maxConcurrent = in.readInt();
			this.messageDuration = in.readInt();
			this.inAnimationResource = in.readInt();
			this.outAnimationResource = in.readInt();

			// re-instantiate queue
			messages = new ArrayList<Message>();
			in.readList(messages, TorchView.class.getClassLoader());
		}

		@SuppressWarnings("NullableProblems")
		@Override
		public void writeToParcel(Parcel out, int flags) {
			super.writeToParcel(out, flags);

			out.writeLong(this.delayBetweenMessages);
			out.writeInt(this.maxConcurrent);
			out.writeInt(this.messageDuration);
			out.writeInt(this.inAnimationResource);
			out.writeInt(this.outAnimationResource);
			out.writeList(this.messages);
		}

		//required field that makes Parcelables from a Parcel
		public static final Parcelable.Creator<SavedState> CREATOR =
				new Parcelable.Creator<SavedState>() {
					public SavedState createFromParcel(Parcel in) {
						return new SavedState(in);
					}

					public SavedState[] newArray(int size) {
						return new SavedState[size];
					}
				};
	}
}
