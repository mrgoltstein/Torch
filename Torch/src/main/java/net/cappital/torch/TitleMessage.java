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

/**
 * Basic message with a title. This class is mainly here to demonstrate how to extend {@link
 * net.cappital.torch.Message}.
 */
public class TitleMessage extends Message {

	private String title;

	public TitleMessage(String title, String message) {
		super(message);
		this.title = title;
	}

	public String getTitle() {
		return title;
	}

	public TitleMessage setTitle(String title) {
		this.title = title;
		return this;
	}

	public TitleMessage(Parcel in) {
		super(in);
		this.title = in.readString();
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		super.writeToParcel(out, flags);
		out.writeString(this.title);
	}

	public static final Creator<Message> CREATOR = new Creator<Message>() {

		@Override
		public Message createFromParcel(Parcel source) {
			return new TitleMessage(source);
		}

		@Override
		public Message[] newArray(int size) {
			return new Message[size];
		}
	};

}
