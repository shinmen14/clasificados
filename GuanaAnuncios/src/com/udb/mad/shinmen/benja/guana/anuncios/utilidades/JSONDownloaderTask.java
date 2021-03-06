package com.udb.mad.shinmen.benja.guana.anuncios.utilidades;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import com.udb.mad.shinmen.benja.guana.anuncios.RegistroActivity;

import android.os.AsyncTask;
import android.util.Log;

/**
 * Ejemplo de uso:<br/><br/>
 * 
 * List<NameValuePair> parametros = new ArrayList<NameValuePair>(1);<br/>
 * parametros.add(new BasicNameValuePair("param", "valor"));<br/>
 * JSONDownloaderTask jdt = new JSONDownloaderTask(url, JSONDownloaderTask.METODO_POST, null);<br/>
 * JSONArray jsonArray = jdt.execute().get();<br/>
 * 
 *
 */
public class JSONDownloaderTask<T> extends AsyncTask<String, String, T> {

	private String url;
	private String metodo;
	private List<NameValuePair> parametrosLista;
	public static final String METODO_POST = "POST";
	public static final String METODO_GET = "GET";
	private final String ENCODING = "utf-8";
	private OnFinishDownload<T> listener;
	private OnFinishDownloadJSONObject<T> listenerJO;
	private OnStartDownload onStartDownloadListener;
	
	private boolean jsonArray;
	
	public interface OnStartDownload{
		public void onStartDownload();
	}
	
	public interface OnFinishDownload<T> {
		public void onFinishDownload(T json);
		public void loadError();
	}
	
	public interface OnFinishDownloadJSONObject<T> {
		public void onFinishDownloadJSONObject(T json);
		public void loadError();
	}
	
	public JSONDownloaderTask(String url, String metodo,
			List<NameValuePair> parametrosLista) {
		this.url = url;
		this.metodo = metodo;
		this.parametrosLista = parametrosLista;
		this.jsonArray = false;
	}
	
	public JSONDownloaderTask(String url, String metodo,
			List<NameValuePair> parametrosLista, boolean jsonArray) {
		this.url = url;
		this.metodo = metodo;
		this.parametrosLista = parametrosLista;
		this.jsonArray = jsonArray;
	}
	
	public JSONDownloaderTask(String url, String metodo,
			List<NameValuePair> parametrosLista, OnFinishDownload<T> listener) {
		this.url = url;
		this.metodo = metodo;
		this.parametrosLista = parametrosLista;
		this.listener =listener;
		this.jsonArray = false;
	}
	
	public JSONDownloaderTask(String url, String metodo,
			List<NameValuePair> parametrosLista, OnFinishDownloadJSONObject<T> listener) {
		this.url = url;
		this.metodo = metodo;
		this.parametrosLista = parametrosLista;
		this.listenerJO =listener;
		this.jsonArray = false;
	}
	
	public void setOnFinishDownload(OnFinishDownload<T> listener) {
		this.listener = listener;
	}
	
	public void setOnFinishDownloadJSONObject(OnFinishDownloadJSONObject<T> listener) {
		this.listenerJO = listener;
	}
	
	public OnStartDownload getOnStartDownloadListener() {
		return onStartDownloadListener;
	}

	public void setOnStartDownloadListener(OnStartDownload onStartDownloadListener) {
		this.onStartDownloadListener = onStartDownloadListener;
	}

	@Override
	protected void onPreExecute() {
		if(onStartDownloadListener != null){
			onStartDownloadListener.onStartDownload();
		}
	}

	@Override
	protected void onPostExecute(T result) {
		//super.onPostExecute(result);
		if(listener != null) {
			if(result != null) {
				listener.onFinishDownload(result);
			} else {
				listener.loadError();
			}
		}else if(listenerJO != null) {
			if(result != null) {
				listenerJO.onFinishDownloadJSONObject(result);
			} else {
				listenerJO.loadError();
			}
		}
	}
	
	
	public boolean isJsonArray() {
		return jsonArray;
	}

	public void setJsonArray(boolean jsonArray) {
		this.jsonArray = jsonArray;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected T doInBackground(String... parametros) {
		HttpResponse response = null;
		HttpEntity entity = null;
		InputStream inputStream = null;
		T jsonData = null;
		String json = "";
		try {
			DefaultHttpClient httpClient = new DefaultHttpClient();
			if (this.metodo.equals(METODO_POST)) {
				HttpPost post = new HttpPost(url);
				if(parametrosLista != null){
					post.setEntity(new UrlEncodedFormEntity(this.parametrosLista));
				}
				response = httpClient.execute(post);
			} else {
				if(parametrosLista != null){
					String paramString = URLEncodedUtils
						      .format(parametrosLista, "utf-8");
				    this.url += "?" + paramString;
				}
			    HttpGet get = new HttpGet(url);
			    response = httpClient.execute(get);
			}
			StatusLine statusLine = response.getStatusLine();
			int statusCode = statusLine.getStatusCode();
			if (statusCode == 200) {
				entity = response.getEntity();
				inputStream = entity.getContent();
				// obtuvimos respuesta, a transformar...
				BufferedReader reader = new BufferedReader(new InputStreamReader(
						inputStream, ENCODING), 8);
				StringBuilder sb = new StringBuilder();
				String line = null;
				while ((line = reader.readLine()) != null) {
					sb.append(line)
					  .append("\n");
				}
				inputStream.close();
				json = sb.toString();
				if(isJsonArray()) {
					// Verificamos la conversion
					if(json.startsWith("[")) {
						jsonData = (T) new JSONArray(json);
					} else {
						Log.e("JSON Downloader", "Se esperaba un array "
								+ "json pero se obtuvo un valor diferente: " 
								+ json);
					}
				} else {
					if(json.startsWith("{")) {
						jsonData = (T) new JSONObject(json);
					} else {
						Log.e("JSON Downloader", "Se esperaba un object "
								+ "json pero sem obtuvo un valor diferente: " 
								+ json);
					}
				}
			} else {
				Log.e(RegistroActivity.class.toString(),
						"Failed to download JSON");
				
				jsonData = (T) new JSONObject("{'estado':'0','errors':{'status':'Status code = "+ statusCode +"'}}");
			}
			
		}catch (Exception e) {
			Log.e("JSON Downloader", "Ocurrio un error al parsear los datos"
					+ e.toString());
		}
		return jsonData;
	}

}
