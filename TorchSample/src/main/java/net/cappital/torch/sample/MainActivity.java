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
package net.cappital.torch.sample;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import net.cappital.torch.Message;
import net.cappital.torch.TitleMessage;
import net.cappital.torch.TorchView;

public class MainActivity extends ActionBarActivity {

	private static final String TAG = "TorchSample";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Log.d(TAG, "onCreate()");

		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment())
					.commit();
		}
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment implements TorchView.ViewBuilder {

		private TorchView simpleTorch;

		private TorchView titleTorch;

		private int messagesShown = 0;
		private Message lastTitleMessage;

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
								 Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container, false);
			return rootView;
		}

		View.OnClickListener btnClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				messagesShown++;

				switch(v.getId()) {
					case R.id.simple_btn:
						showSimpleMessage();
						break;
					case R.id.title_btn:
						showTitleMessage();
						break;
					case R.id.dismiss_btn:
						dismissLastMessage();
						break;
				}
			}
		};

		private static final String TORCH_SIMPLE = "torch_simple";
		private static final String TORCH_TITLE = "torch_title";

		@Override
		public void onViewCreated(View view, Bundle savedInstanceState) {
			view.findViewById(R.id.simple_btn).setOnClickListener(btnClickListener);
			view.findViewById(R.id.title_btn).setOnClickListener(btnClickListener);
			view.findViewById(R.id.dismiss_btn).setOnClickListener(btnClickListener);

			simpleTorch = (TorchView) view.findViewById(0x8281def);
			titleTorch = (TorchView) view.findViewById(0x7271def);

			if (simpleTorch == null) {
				// uses default View with custom background
				simpleTorch = new TorchView(getActivity(), 5, TorchView.LENGTH_SHORT);
				simpleTorch.attach(getActivity());
				simpleTorch.setBackgroundResource(android.R.color.holo_blue_bright);
			} else {
				simpleTorch.show();
			}

			if (titleTorch == null) {
				Log.d(TAG, "new title");
				titleTorch = new TorchView(getActivity(), 2, TorchView.LENGTH_LONG);
				titleTorch.attach(getActivity());
				titleTorch.setViewBuilder(this);
				titleTorch.setInAnimationResource(R.anim.torch_wrapper_in);
				titleTorch.setOutAnimationResource(R.anim.torch_wrapper_out);
				titleTorch.setViewBuilder(this);

				// set ID to remember state across orientation-change
				titleTorch.setId(0x7271def);
			} else {
				Log.d(TAG, "reuse existing title");
				titleTorch.setViewBuilder(this);
				titleTorch.show();
			}
		}

		private void showSimpleMessage() {
			Message message = new Message("Message #"+messagesShown+"...");
			// shows message when it reaches the top of queue
			simpleTorch.show(message);
		}

		void showTitleMessage() {
			// You can use custom classes that extend message and access them in ViewBuilder
			Message message = new TitleMessage("Title for message", "Message #" + messagesShown + "...");
			// enable swipe-to-dismissView
			message.setDismissable(true);
			titleTorch.show(message);
			
			lastTitleMessage = message;
		}

		void dismissLastMessage() {
			if (lastTitleMessage != null) {
				//titleTorch.dismissView(lastTitleMessage);
				lastTitleMessage = null;
				Toast.makeText(getActivity(), "Ignored", Toast.LENGTH_SHORT).show();
			}
		}

		@Override
		public View getView(Context context, Message message) {
			return getTitleView(context, message);
		}

		View getTitleView(Context context, Message m) {
			TitleMessage message = (TitleMessage) m;

			BlockButton btn = new BlockButton(context);
			btn.setTitle(message.getTitle());
			btn.setText(message.getContent());
			btn.setIconResource(R.drawable.ic_action_undo);
			btn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Toast.makeText(getActivity(), "Clicked message!", Toast.LENGTH_SHORT).show();
				}
			});
			btn.setOnIconClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Toast.makeText(getActivity(), "Clicked icon!", Toast.LENGTH_SHORT).show();
				}
			});

			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			lp.topMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, context.getResources().getDisplayMetrics());
			btn.setLayoutParams(lp);

			return btn;
		}
	}

}
