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

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Basic class that is used by {@link TorchView} to show messages.
 * <p/>
 * To extend {@link TorchView} behavior, extend this class and access it by casting in {@link
 * net.cappital.torch.TorchView.ViewBuilder#getView(Message)} or, if you have overridden {@link
 * TorchView}, in {@link TorchView#getView(Message)}.
 * <p/>
 * If you want your custom {@link net.cappital.torch.Message} to survive orientation-change, make
 * sure to override {@link #writeToParcel(android.os.Parcel, int)} and call {@link
 * #Message(android.os.Parcel)} in your Parcelable-constructor. See {@link
 * net.cappital.torch.TitleMessage} for an example!
 */
@SuppressWarnings("unused")
public class Message implements Parcelable {

	private int id;

	private int viewType;

	private String content;

	private int inAnimationResource = R.anim.torch_message_in_default;

	private int outAnimationResource = R.anim.torch_message_out_default;

	private boolean isDismissable = false;

	public Message(String contents) {
		this.content = contents;
	}

	/**
	 * Get the ID of this message. Messages are only added to the queue if their ID is unique in the
	 * queue, or 0.
	 *
	 * @return The ID of this message
	 */
	public int getId() {
		return id;
	}

	/**
	 * Set the ID of this message. Messages are only added to the queue if their ID is unique in the
	 * queue, or 0.
	 *
	 * @param id The ID of this message (must be 0 or unique in the queue)
	 *
	 * @return This Message for method chaining.
	 */
	public Message setId(int id) {
		this.id = id;
		return this;
	}

	/**
	 * Get the view-type for this message. Use this for example to distinguish between different
	 * types of Views you want to construct for different instances of Torch, if these instances use
	 * the same {@link net.cappital.torch.TorchView.ViewBuilder}.
	 *
	 * @return int indicating the view-type
	 */
	public int getViewType() {
		return viewType;
	}

	/**
	 * Set the view-type for this message. See {@link #getViewType()}.
	 *
	 * @param viewType Arbitrary int that defines the view-type.
	 *
	 * @return This object for method chaining.
	 */
	public Message setViewType(int viewType) {
		this.viewType = viewType;
		return this;
	}

	/**
	 * Get the message to display.
	 *
	 * @return The message-String to show.
	 */
	public String getContent() {
		return content;
	}

	/**
	 * Sets the message to display.
	 *
	 * @param content The String-message to display
	 *
	 * @return This object for method chaining.
	 */
	public Message setContent(String content) {
		this.content = content;
		return this;
	}

	/**
	 * Get the in-animation for this message.
	 *
	 * @return Resource ID of the in-animation.
	 */
	public int getInAnimationResource() {
		return inAnimationResource;
	}

	/**
	 * Replaces the default in-animation for messages. <B>NOTE:</B> this is ignored if {@link
	 * net.cappital.torch.TorchView} is only showing 1 message.
	 *
	 * @param inAnimationResource The animation-resource
	 *
	 * @return This Message for method chaining.
	 */
	public Message setInAnimationResource(int inAnimationResource) {
		this.inAnimationResource = inAnimationResource;
		return this;
	}

	/**
	 * Get the out-animation for this message.
	 *
	 * @return Resource ID of the out-animation.
	 */
	public int getOutAnimationResource() {
		return outAnimationResource;
	}

	/**
	 * Replaces the default out-animation for messages. <B>NOTE:</B> this is ignored if {@link
	 * net.cappital.torch.TorchView} is only showing 1 message.
	 *
	 * @param outAnimationResource The animation-resource
	 *
	 * @return This Message for method chaining.
	 */
	public Message setOutAnimationResource(int outAnimationResource) {
		this.outAnimationResource = outAnimationResource;
		return this;
	}

	/**
	 * Returns whether this message can be dismissed by swiping it off the screen.
	 *
	 * @return {@code true} if message is dismissable, {@code false} if not.
	 */
	public boolean isDismissable() {
		return isDismissable;
	}

	/**
	 * Set whether this message can be dismissed by swiping it off the screen.
	 *
	 * @param dismissable Whether this Message can be dismissed by swiping
	 *
	 * @return This object for method chaining.
	 */
	public Message setDismissable(boolean dismissable) {
		this.isDismissable = dismissable;
		return this;
	}

	public Message(Parcel in) {
		this.id = in.readInt();
		this.viewType = in.readInt();
		this.content = in.readString();
		this.inAnimationResource = in.readInt();
		this.outAnimationResource = in.readInt();
		this.isDismissable = in.readInt() == 1;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeInt(this.id);
		out.writeInt(this.viewType);
		out.writeString(this.content);
		out.writeInt(this.inAnimationResource);
		out.writeInt(this.outAnimationResource);
		out.writeInt(this.isDismissable ? 1 : 0);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null) {
			return false;
		}

		if (!(o instanceof Message)) {
			return false;
		}

		Message m = (Message) o;
		if (id != m.id) {
			return false;
		}
		if (viewType != m.viewType) {
			return false;
		}

		return content.equals(m.content);
	}

	public static final Creator<Message> CREATOR = new Creator<Message>() {

		@Override
		public Message createFromParcel(Parcel source) {
			return new Message(source);
		}

		@Override
		public Message[] newArray(int size) {
			return new Message[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

}
