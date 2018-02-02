package chen.zhaohui.wifipositioning;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by 陈朝晖 on 2017/5/10.
 */
public class SurveyScanFragment extends Fragment 
	implements OnClickListener, OnItemClickListener {

	private View view;
	protected WifiData wData;
	protected WifiScanner wScanner;
	protected ListView listScan;
	protected Button btnScan, btnDel;
	protected ToggleButton toggleSelect;
	protected SeparatedListAdapter adapterList, adapterSelect;
	protected ProgressDialog pDialog;
	protected int curPointId = 0;
	protected ArrayList<Integer> listSurveyId;
	protected Activity activity;
	protected int gridSize, gridUnit;
	protected boolean isScanning;

	public final static String ITEM_TITLE = "title";
	public final static String ITEM_CAPTION = "caption";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		activity = getActivity();
		wData = WifiData.getInstance(activity);
		wScanner = WifiScanner.getInstance(activity);
		listSurveyId = new ArrayList<Integer>();
		gridUnit = wData.getGridUnit();
    	gridSize = wData.getGridSize();

//		wData.reFresh();
//		//更新数据库
//		Log.e("SurveyFragment", "更新数据库" );
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Log.d("SurveyListFragment", "onCreateView");

//		wData.reFresh();
//		//更新数据库
//		Log.e("SurveyFragment", "更新数据库" );

		if (view == null) {
			view = inflater.inflate(R.layout.survey_scan_fragment, container, false);
			
			btnScan = (Button)view.findViewById(R.id.btnScan);
			btnScan.setOnClickListener(this);
			btnDel = (Button)view.findViewById(R.id.btnDel);
			btnDel.setOnClickListener(this);
			toggleSelect = (ToggleButton)view.findViewById(R.id.toggleSelect);
			toggleSelect.setChecked(false);
			toggleSelect.setOnClickListener(this);
			listScan = (ListView)view.findViewById(R.id.listScan);
			listScan.setOnItemClickListener(this);
			
			if (curPointId == 0) {
				btnScan.setVisibility(View.GONE);
			}
			
			loadSurveyListview();
			
			if (curPointId != 0 && listSurveyId.size() <= 1) {
				Toast.makeText(getActivity(), "点击 '扫描' 开始WiFi扫描", Toast.LENGTH_LONG).show();
			}
		}

		return view;
	}
	
	@Override 
	public void onDestroyView() { 
		super.onDestroyView(); 
		ViewGroup parentViewGroup = (ViewGroup) view.getParent(); 
		if( null != parentViewGroup ) { 
			parentViewGroup.removeView( view ); 
		} 
	}
	
	public Map<String, ?> createItem(String title, String caption) {
		Map<String, String> item = new HashMap<String, String>();
		item.put(ITEM_TITLE, title);
		item.put(ITEM_CAPTION, caption);
		return item;
	}

	public void setCurPointId(int pid) {//SurveyFregment传递的pid值
		curPointId = pid;
	}
	
	public void loadSurveyListview() {
		if(wScanner.isWifiEnabled()) {
			wData.reFresh();
			//更新数据库
			Log.e("SurveyFragment", "更新数据库");
		}
		// 取消参考点的选中状态 并创建新的适配器
		adapterList = null;
		adapterSelect = null;
		adapterList = new SeparatedListAdapter(activity);
		adapterSelect = new SeparatedListAdapter(activity);



		ArrayList<WifiData.Point> pList = null;
		if (curPointId == 0)
			pList = wData.getPoints();
		else {
			pList = new ArrayList<WifiData.Point>();
			pList.add(wData.getPoint(curPointId));
		}
		
		listSurveyId.clear();
		for (WifiData.Point p : pList) {
			String sHeader = "P"+p.pid+"("+wData.formatDecimal(p.px*gridUnit/gridSize)+" , "+wData.formatDecimal(p.py*gridUnit/gridSize)+")米  编号="+p.cnt_survey;
			ArrayList<WifiData.Survey> listSurvey = wData.getSurveysByPoint(p.pid);
			String[] strArrSurvey = new String[listSurvey.size()];
			listSurveyId.add(0);
			for (int i=0; i<listSurvey.size(); i++) {
				strArrSurvey[i] = "S"+listSurvey.get(i).sid+" "+wData.formatDatetime(listSurvey.get(i).sts)+" #SID:"+listSurvey.get(i).cnt_sample;
				listSurveyId.add(listSurvey.get(i).sid);
			}
			
			// create our list and custom adapter
			adapterList.addSection(sHeader, new ArrayAdapter<String>(activity,
					android.R.layout.simple_list_item_1, strArrSurvey));
			adapterSelect.addSection(sHeader, new ArrayAdapter<String>(activity,
					android.R.layout.simple_list_item_multiple_choice, strArrSurvey));
		}
		
		// reset the select toggle status 
		toggleSelect.setChecked(false);
		listScan.setAdapter(adapterList);
	}

	public void onClick(View v) {

//		wData.reFresh();
//		//更新数据库
//		Log.e("SurveyFragment", "更新数据库" );

		if (v == btnScan) {
			if (!wScanner.isWifiEnabled()) {
				Toast.makeText(activity, "请开启设备WiFi", Toast.LENGTH_LONG).show();
				return;
			}
			
			isScanning = true;
			pDialog = new ProgressDialog(activity);
			pDialog.setMessage("WiFi 探测中...");
			//pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			pDialog.setCancelable(false);
			pDialog.setCanceledOnTouchOutside(false);
			pDialog.setButton(DialogInterface.BUTTON_NEUTRAL, "停止", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					pDialog.getButton(which).setVisibility(View.INVISIBLE);
				}
			});
			pDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
				public void onDismiss(DialogInterface dialog) {
					if (isScanning) {
						pDialog.setMessage("正在停止WiFi探测, 请稍候...");
						pDialog.show();
						isScanning = false;
					}
				}
			});
			pDialog.show();
			new Thread(new Runnable() {
				public void run() {
					// 在新线程中执行WiFi扫描
					while (isScanning) {
						ArrayList<List<ScanResult>> sLists = new ArrayList<List<ScanResult>>();
						for (int i=0; i<WifiData.NUM_WIFI_CATPURE; i++) {
							List<ScanResult> sList = wScanner.scanWifi();
							if (sList != null) {
								Log.d("SurveyScanFragment", "scanWifi #"+i+" return "+sList.size()+" record");
								sLists.add(sList);
							}
							if (i+1 < WifiData.NUM_WIFI_CATPURE)
								wScanner.sleep(WifiData.WIFI_SURVEY_INTERVAL); // scan间间隔2秒
						}
						
						ArrayList<WifiData.Sample> mList = wScanner.getMeanSample(sLists);
						//均值滤波
						int sid = wData.insertSurvey(curPointId, mList);

						Log.d("SurveyScanFragment", "survey id "+sid+" created");

//						wData.reFresh();
//						//更新数据库
//						Log.e("SurveyFragment", "更新数据库" );

						// update UI in UI thread to prevent exception
						activity.runOnUiThread(new Runnable() {public void run() {
							loadSurveyListview();
						}});

						if (isScanning)
							wScanner.sleep(30 * 1000);
					}
					
					wScanner.sleep(1000);
					pDialog.dismiss();
				}
			}).start();
		} else if (v == toggleSelect) {
			if (toggleSelect.isChecked()) {
				listScan.setAdapter(adapterSelect);
			} else {
				listScan.setAdapter(adapterList);
			}
		} else if (v == btnDel) {
			SparseBooleanArray checked = listScan.getCheckedItemPositions();
			for (int i=0; i<listScan.getCount(); i++) {
				if (checked.get(i)) {
					Log.d("SurveyScanFragment", "Remove survey ID "+listSurveyId.get(i));
					wData.removeSurvey(listSurveyId.get(i));
				}
			}
			loadSurveyListview();
		}
	}

	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		if (!toggleSelect.isChecked()) {
			Log.d("SurveyScanFragment", "Survey ID "+listSurveyId.get(arg2)+" clicked");
	        FragmentTransaction ft = getFragmentManager().beginTransaction();
	        SurveyListFragment listFrag = new SurveyListFragment();//SurveyListFragment是Fragment的list类
	        listFrag.setSurvey(listSurveyId.get(arg2));
	        ft.replace(R.id.container, listFrag, MainMenu.TAG_SURVEY_LIST_FRAGMENT);
	        ft.addToBackStack(null);
	        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
	        ft.commit();
		}
	}
}
