/*
 * RestClient.java
 *
 * Provides a complete Android REST client.
 *
 * Copyright (c) 2012 Sutee Sudprasert.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Software written by:
 * Sutee Sudprasert  <sutee.s@gmail.com>
 */

package th.ac.ku.sci.cs.android.sutee.imagedownloader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.AbstractHttpMessage;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import android.os.Handler;
import android.util.Log;

/**
 * RestClient.java
 * Credit to Luke Lowrey for his code for RestClient which makes call to
 * web services in a pretty neat way. Here is the code for RestClient class
 * that uses org.apache.http library which is included in Android.
 *
 * @see http://lukencode.com/2010/04/27/calling-web-services-in-android-using-httpclient/
 * 
 * @author Luke Lowrey
 * @author Sutee Sudprasert (refactoring and improvement)
 */

public class RestClient {

	public static enum RequestMethod {
		GET, POST, PUT, DELETE
	}

	public abstract static class RestMethod {
		public abstract RequestMethod getMethodType();

		public abstract void executeRequest(RestClient client)
				throws UnsupportedEncodingException;
	}

	public static class GetMethod extends RestMethod {

		@Override
		public RequestMethod getMethodType() {
			return RequestMethod.GET;
		}

		@Override
		public void executeRequest(RestClient client)
				throws UnsupportedEncodingException {
			// add parameters
			String combinedParams = "";
			if (!client.params.isEmpty()) {
				combinedParams += "?";
				for (NameValuePair p : client.params) {
					String paramString = p.getName() + "="
							+ URLEncoder.encode(p.getValue(), "UTF-8");
					if (combinedParams.length() > 1) {
						combinedParams += "&" + paramString;
					} else {
						combinedParams += paramString;
					}
				}
			}
			HttpGet request = new HttpGet(client.url + combinedParams);
			client.addHeaders(request);
			client.executeRequest(request, client.url, getMethodType());
		}
	}

	public static class PostMethod extends RestMethod {

		@Override
		public RequestMethod getMethodType() {
			return RequestMethod.POST;
		}

		@Override
		public void executeRequest(RestClient client)
				throws UnsupportedEncodingException {
			HttpPost request = new HttpPost(client.url);
			client.addHeaders(request);

			if (!client.params.isEmpty()) {
				request.setEntity(new UrlEncodedFormEntity(client.params,
						HTTP.UTF_8));
			}

			client.executeRequest(request, client.url, getMethodType());
		}

	}

	public static class DeleteMethod extends RestMethod {

		@Override
		public RequestMethod getMethodType() {
			return RequestMethod.DELETE;
		}

		@Override
		public void executeRequest(RestClient client)
				throws UnsupportedEncodingException {
			HttpDelete request = new HttpDelete(client.url);
			client.addHeaders(request);

			client.executeRequest(request, client.url, getMethodType());
		}

	}

	public static class PutMethod extends RestMethod {

		@Override
		public RequestMethod getMethodType() {
			return RequestMethod.PUT;
		}

		@Override
		public void executeRequest(RestClient client)
				throws UnsupportedEncodingException {
			HttpPut request = new HttpPut(client.url);
			client.addHeaders(request);

			if (!client.params.isEmpty()) {
				request.setEntity(new UrlEncodedFormEntity(client.params,
						HTTP.UTF_8));
			}

			client.executeRequest(request, client.url, getMethodType());
		}

	}

	public static class RestMethodFactory {
		public static RestMethod create(RequestMethod requestMethod) {
			switch (requestMethod) {
			case GET:
				return new GetMethod();
			case POST:
				return new PostMethod();
			case DELETE:
				return new DeleteMethod();
			case PUT:
				return new PutMethod();
			default:
				throw new IllegalArgumentException("Unsupported Method: "
						+ requestMethod.toString());
			}
		}
	}

	public static interface OnRequestFinishListener {
		void onRequestFinish(RequestMethod method, int responseCode,
				String message, String response);

		void onRequestFinishWithError(Exception e);
	}

	private OnRequestFinishListener onRequestFinishListener;

	private Handler mHandler = new Handler();

	private ArrayList<NameValuePair> params;
	private ArrayList<NameValuePair> headers;

	private String url;

	public RestClient(String url) {
		this.url = url;
		params = new ArrayList<NameValuePair>();
		headers = new ArrayList<NameValuePair>();
	}

	public void setOnRequestFinishListener(
			OnRequestFinishListener onRequestFinishListener) {
		this.onRequestFinishListener = onRequestFinishListener;
	}

	public void addParam(String name, String value) {
		params.add(new BasicNameValuePair(name, value));
	}

	public void addHeader(String name, String value) {
		headers.add(new BasicNameValuePair(name, value));
	}

	public void execute(RequestMethod method) {
		try {
			RestMethodFactory.create(method).executeRequest(this);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	private void addHeaders(AbstractHttpMessage request) {
		for (NameValuePair h : headers) {
			request.addHeader(h.getName(), h.getValue());
		}
	}

	private void notifyRequestFinish(RequestMethod method, int responseCode,
			String message, String response) {
		if (onRequestFinishListener != null) {
			onRequestFinishListener.onRequestFinish(method, responseCode,
					message, response);
		}
	}

	private void notifyRequestFinishWithError(Exception e) {
		if (onRequestFinishListener != null) {
			onRequestFinishListener.onRequestFinishWithError(e);
		}
	}

	private void executeRequest(final HttpUriRequest request, String url,
			final RequestMethod method) {
		new Thread(new Runnable() {
			public void run() {
				final HttpClient client = new DefaultHttpClient();

				try {
					final HttpResponse httpResponse = client.execute(request);
					Log.d("RestClient", httpResponse.getAllHeaders().toString());
					String response = null;
					if (httpResponse.getEntity() != null) {
						try {
							InputStream instream = httpResponse.getEntity()
									.getContent();
							response = convertStreamToString(instream);
							instream.close();
						} catch (IllegalStateException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					final String returnResponse = response;
					mHandler.post(new Runnable() {
						public void run() {
							notifyRequestFinish(method, httpResponse
									.getStatusLine().getStatusCode(),
									httpResponse.getStatusLine()
											.getReasonPhrase(), returnResponse);
						}
					});
				} catch (final ClientProtocolException e) {
					client.getConnectionManager().shutdown();
					e.printStackTrace();
					mHandler.post(new Runnable() {
						public void run() {
							notifyRequestFinishWithError(e);
						}
					});
				} catch (final IOException e) {
					client.getConnectionManager().shutdown();
					e.printStackTrace();
					mHandler.post(new Runnable() {
						public void run() {
							notifyRequestFinishWithError(e);
						}
					});
				}
			}
		}).start();
	}

	private static String convertStreamToString(InputStream is) {

		BufferedReader reader = new BufferedReader(new InputStreamReader(is),
				8192);
		StringBuilder sb = new StringBuilder();

		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return sb.toString();
	}

}