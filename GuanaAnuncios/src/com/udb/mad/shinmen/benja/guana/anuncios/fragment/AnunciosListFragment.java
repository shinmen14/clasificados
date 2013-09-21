package com.udb.mad.shinmen.benja.guana.anuncios.fragment;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;

import com.udb.mad.shinmen.benja.guana.anuncios.LoginActivity;
import com.udb.mad.shinmen.benja.guana.anuncios.adapters.AnuncioCustomAdapter;
import com.udb.mad.shinmen.benja.guana.anuncios.listeners.EndlessScrollListener;
import com.udb.mad.shinmen.benja.guana.anuncios.listeners.EndlessScrollListener.onScrollEndListener;
import com.udb.mad.shinmen.benja.guana.anuncios.model.Anuncio;
import com.udb.mad.shinmen.benja.guana.anuncios.utilidades.JSONDownloaderTask;
import com.udb.mad.shinmen.benja.guana.anuncios.utilidades.PreferenciasUsuario;

public class AnunciosListFragment extends ListFragment implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8964105318347238707L;

	EndlessScrollListener scrollListener;
	List<Anuncio> anuncios;
	AnuncioCustomAdapter adapter;
	Activity activity;
	//SharedPreferences prefs;
	String token;
	String usuario;
	String query;
	String PAGE_MARK = "{page}";
	String LIMIT_MARK = "{limit}";
	int page = 0;
	int limit = 8;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (PreferenciasUsuario.isUsuarioAutenticado(getActivity())) {
			anuncios = new ArrayList<Anuncio>();
//			prefs = getActivity().getSharedPreferences(
//					"GuanaAnunciosPreferences", Context.MODE_PRIVATE);
			token = PreferenciasUsuario.getToken(getActivity());
			usuario = PreferenciasUsuario.getUsuario(getActivity());
			
			if (adapter == null) {
				adapter = new AnuncioCustomAdapter(getActivity());
			}
			setListAdapter(adapter);
			scrollListener = new EndlessScrollListener(
					new onScrollEndListener() {
						@Override
						public void onEnd(int page) {
							cargarAnuncios(page);
						}
					});
			getListView().setOnScrollListener(scrollListener);
			setListShownNoAnimation(false);
			cargarAnuncios(page);
		} else {
			// Se tiene que levantar la actividad de login y finalizar esta
			// actividad.
			showLogin();
		}
	}

	private void showLogin() {
		Intent i = new Intent(getActivity(),LoginActivity.class);
		startActivity(i);
		getActivity().finish();
	}

	private void cargarAnuncios(int page) {
		
		this.page = page;

		List<NameValuePair> parametros = new ArrayList<NameValuePair>(2);
		parametros.add(new BasicNameValuePair("usuario", usuario));
		parametros.add(new BasicNameValuePair("token", token));
		parametros.add(new BasicNameValuePair("texto", query));

		String url = "http://guananuncio.madxdesign.com/index.php/anuncio/anunciosbusqueda/{page}/{limit}";

		url = url.replace(PAGE_MARK, page + "");
		url = url.replace(LIMIT_MARK, limit + "");

		JSONDownloaderTask<JSONArray> jdt = new JSONDownloaderTask<JSONArray>(
				url, JSONDownloaderTask.METODO_POST, parametros, true);
		jdt.setOnFinishDownload(new BusquedaAnunciosListener());

		jdt.execute();

	}

	public void refrescarLista(){
		setListShownNoAnimation(false);
		query = null;
		page = 0;
		adapter.clear();
		scrollListener.setCurrentPage(page);
		scrollListener.setPreviousTotal(0);
		cargarAnuncios(page);
	}
	
	private class BusquedaAnunciosListener implements
			JSONDownloaderTask.OnFinishDownload<JSONArray> {

		@Override
		public void onFinishDownload(JSONArray json) {
			Anuncio anuncio;
			
			try {
				for (int i = 0; i < json.length(); i++) {

					JSONObject jsonObject = json.getJSONObject(i);

					anuncio = new Anuncio();
					anuncio.setCodigoAnuncio(jsonObject.getString("id"));
					anuncio.setTituloAnuncio(jsonObject.getString("titulo"));
					anuncio.setDescripcionAnuncio(jsonObject.getString("descripcion"));
					adapter.add(anuncio);
				}
			} catch (Exception e) {
				Log.e("error", e.getMessage());
			}
			
			adapter.notifyDataSetChanged();
			AnunciosListFragment.this.setListShownNoAnimation(true);
		}

		@Override
		public void loadError() {
			// supondremos que el error es porque no te has autenticado.
			showLogin();
		}
	}
}