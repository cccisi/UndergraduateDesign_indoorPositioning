package chen.zhaohui.wifipositioning;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
/**
 * Created by 陈朝晖 on 2017/5/10.
 */
public class PositioningFragment extends Fragment
	implements FloorPlanView.OnFloorPlanClickListener,
	OrientationSensor.OnSensorChangeListener, OnClickListener {
	
	public static final String kEmptyEstCoordStr = "P(__ , __)米  方向:__°";
	public static final long kRotateInterval = 200;
	public static final float kRotateAdjust = 20;
	
	private View view;
	protected WifiData wData;
	protected WifiScanner wScanner;
	protected FloorPlanView viewFloorplan;
	protected int gridSize, gridUnit;
	protected OrientationSensor sensor;
	protected TextView txtEstCoord;
	protected float curDirection;
	protected ToggleButton toggleScan, toggleGrid, togglePoint, toggleRotate;
	protected ToggleButton toggleTrustRegion, toggleOrientationFilter;
	protected Button btnInfo;
	protected Timer scanTimer;
	protected Activity activity;
	protected ArrayList<WifiData.PositionSurvey> listSurvey;
	protected WifiData.PositionTemp curPoint, lastPoint, newPoint;
	//cur当前，coord坐标
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
        // this fragment has own option menu
        setHasOptionsMenu(true);
        
        activity = getActivity();
        wData = WifiData.getInstance(activity);
        wScanner = WifiScanner.getInstance(activity);
    	sensor = new OrientationSensor(activity);
    	sensor.setOnSensorChangeListener(this);
		gridUnit = wData.getGridUnit();
    	gridSize = wData.getGridSize();
	}

    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
    	Log.d("PositioningFragment", "onCreateView");
    	
    	if (view == null) {
        	view = inflater.inflate(R.layout.position_fragment, container, false);
        	
        	gridUnit = wData.getGridUnit();
        	gridSize = wData.getGridSize();
        	Bitmap floorplanImg = wData.getFloorplanImage();
        	Log.d("PositioningFragment", "floorplan w="+floorplanImg.getWidth()+",h="+floorplanImg.getHeight());
        	viewFloorplan = (FloorPlanView)view.findViewById(R.id.viewFloorPlan);
        	viewFloorplan.setImageBitmap(floorplanImg);
        	viewFloorplan.setGrid(gridSize, gridUnit);
        	viewFloorplan.setLockGrid(false);
        	viewFloorplan.showTrack(true);
        	viewFloorplan.setOnFloorPlanClickListener(this);
        	
        	txtEstCoord = (TextView)view.findViewById(R.id.txtEstCoord);
        	txtEstCoord.setText(kEmptyEstCoordStr);
        	
        	toggleScan = (ToggleButton)view.findViewById(R.id.toggleScan);
        	toggleScan.setChecked(false);//默认不选择
        	toggleScan.setOnClickListener(this);
        	
        	toggleRotate = (ToggleButton)view.findViewById(R.id.toggleRotate);
        	toggleRotate.setChecked(false);
        	toggleRotate.setOnClickListener(this);
        	viewFloorplan.showRotate(toggleRotate.isChecked());
        	
        	toggleTrustRegion = (ToggleButton)view.findViewById(R.id.toggleTrustRegion);
        	toggleTrustRegion.setChecked(true);
        	toggleTrustRegion.setOnClickListener(this);
        	viewFloorplan.showTrustRegion(toggleTrustRegion.isChecked());
        	
        	toggleOrientationFilter = (ToggleButton)view.findViewById(R.id.toggleOrientationFilter);
        	toggleOrientationFilter.setChecked(true);
        	toggleOrientationFilter.setOnClickListener(this);
        	viewFloorplan.showOrientationFilter(toggleOrientationFilter.isChecked());
        	
        	togglePoint = (ToggleButton)view.findViewById(R.id.togglePoint);
        	togglePoint.setChecked(true);
        	togglePoint.setOnClickListener(this);
        	viewFloorplan.showPoint(togglePoint.isChecked());
        	
        	toggleGrid = (ToggleButton)view.findViewById(R.id.toggleGrid);
        	toggleGrid.setChecked(true);
        	toggleGrid.setOnClickListener(this);
        	viewFloorplan.showGrid(toggleGrid.isChecked());
        	
        	btnInfo = (Button)view.findViewById(R.id.btnInfo);
        	btnInfo.setOnClickListener(this);
        	
        	// 从数据库WiFi DB导入,并导入平面图
        	ArrayList<WifiData.Point> pList = wData.getPoints();
        	for (WifiData.Point p: pList)
    			viewFloorplan.addPoint(p.pid, p.px, p.py, 0, wData.getImgGreenDot(), wData.getImgGreenDot(), wData.getImgGreenDot());
    	}
    	return view;
    }

	@Override
	public void onResume() {
		super.onResume();
		Log.d("PositioningFragment", "onResume");
		
		activity.getWindow().setTitle(MainMenu.TITLE_POSITIONING);
		Toast.makeText(activity, "点击 '开始' 开始WiFi定位\n\n"+
				"点击 '方向' 测量实时运动方向\n\n"+
				"将自己已知的位置标于平面图可辅助定位", Toast.LENGTH_LONG).show();
		
		if (listSurvey == null)
			listSurvey = wData.getPositionSurveys();
		
		sensor.startSensor();//默认打开方向传感器
	}

	@Override
	public void onPause() {
		Log.d("PositioningFragment", "onPause");
		sensor.stopSensor();
		
		if (toggleScan.isChecked())
			stopWifiScan();
		
		super.onPause();
	}

	public void OnFloorPlanClick(int pointId, float posX, float posY) {
		Log.d("PositioningFragment", "OnFloorPlanClick id="+pointId+", X="+posX+", Y="+posY);
		
		// 手工预测坐标点
		float curDegree = curDirection;
		lastPoint = curPoint;
		curPoint = null;
		
		if (toggleOrientationFilter.isChecked()) {
			ArrayList<WifiData.PositionSurvey> listFilter = wScanner.getOrientationFilteredPositionSurveys(listSurvey, lastPoint, curDegree, WifiData.OF_HALF_ANGLE);
			// if orientation filter has survey return, here is for checking only
			if (listFilter != null && listFilter.size() > 0)
				Log.d("PositioningFragment", "OnFloorPlanClick: OrientationFiltered survey size="+listFilter.size());
			else
				Log.d("PositioningFragment", "OnFloorPlanClick: full survey size="+listSurvey.size());
		}

		// 人工定位坐标跳过KNN步骤

		curPoint = wData.newPositionTemp();
		curPoint.px = posX;
		curPoint.py = posY;
		curPoint.pAdjX = posX;
		curPoint.pAdjY = posY;
		curPoint.degree = curDegree;
		
		if (toggleTrustRegion.isChecked())
			wScanner.calcTrustRegion(curPoint, lastPoint);
		
		updateEstCoordText(curPoint.degree);
		viewFloorplan.addTrack(curPoint.px, curPoint.py, curPoint.pAdjX, curPoint.pAdjY, curPoint.radius, curPoint.degree);
		viewFloorplan.invalidate();
	}

	public synchronized void updateEstCoordText(float degree) {
		if (curPoint != null)
			txtEstCoord.setText("P("+wData.formatDecimal(curPoint.pAdjX*gridUnit/gridSize)+" , "+wData.formatDecimal(curPoint.pAdjY*gridUnit/gridSize)+")米  方向:"+wData.formatDecimal(degree)+"°");
		else
			txtEstCoord.setText("P(__ , __)米  方向:"+wData.formatDecimal(degree)+"°");
		txtEstCoord.invalidate();
	}

	public void onClick(View v) {
		if (v == toggleScan) {
			if (!wScanner.isWifiEnabled()) {
				Toast.makeText(activity, "请开启WiFi", Toast.LENGTH_LONG).show();
				return;
			}
			
			if (toggleScan.isChecked()) {
				startWifiScan();
			} else {
				stopWifiScan();
			}
		} else if (v == toggleRotate) {
			viewFloorplan.showRotate(toggleRotate.isChecked());
			viewFloorplan.invalidate();
		} else if (v == toggleTrustRegion) {
			viewFloorplan.showTrustRegion(toggleTrustRegion.isChecked());
			viewFloorplan.invalidate();
		} else if (v == toggleOrientationFilter) {
			viewFloorplan.showOrientationFilter(toggleOrientationFilter.isChecked());
			viewFloorplan.invalidate();
		} else if (v == toggleGrid) {
			viewFloorplan.showGrid(toggleGrid.isChecked());
			viewFloorplan.invalidate();
		} else if (v == togglePoint) {
			viewFloorplan.showPoint(togglePoint.isChecked());
			viewFloorplan.invalidate();
		} else if (v == btnInfo) {
			AlertDialogFragment dialog = AlertDialogFragment.newInstance(0, "WiFi定位指南",
				"点击 '开始' 开始WiFi定位\n"+
				"点击 '测向' 测量实时运动方向\n"+
				"点击 '误差' 显示可信定位区域\n"+
				"点击 '方向' 显示运动方向\n"+
				"点击 '点' 显示测量点\n"+
				"点击 '格' 显示栅格\n\n"+
				"将自己已知的位置标于平面图可辅助定位").setSingleButton();
			dialog.show(getFragmentManager(), MainMenu.TAG_INFO_DIALOG_FRAGMENT);
		}
	}
	
	public void startWifiScan() {
		toggleScan.setChecked(true);
		scanTimer = new Timer(true);
		scanTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				float curDegree = curDirection;
				newPoint = null;
				ArrayList<WifiData.PositionSurvey> listFilter = null;
				List<ScanResult> listScan = wScanner.scanWifi();
				
				if (toggleOrientationFilter.isChecked())
					listFilter = wScanner.getOrientationFilteredPositionSurveys(listSurvey, curPoint, curDegree, WifiData.OF_HALF_ANGLE);
				if (listFilter != null && listFilter.size() > 0) {
					// if orientation filter has survey return
					Log.d("startWifiScan", "OrientationFiltered survey size="+listFilter.size());
					newPoint = wScanner.calcKMeansPoint(listFilter, listScan);
				}
				
				if (newPoint == null) {
					// do full list scan if orientation filter without points
					Log.d("startWifiScan", "full survey size="+listSurvey.size());
					newPoint = wScanner.calcKMeansPoint(listSurvey, listScan);
				}

				if (newPoint != null) {
					newPoint.degree = curDegree;
					if (toggleTrustRegion.isChecked())
						wScanner.calcTrustRegion(newPoint, curPoint);

					// UI线程更新UI
					activity.runOnUiThread(new Runnable() {public void run() {
						lastPoint = curPoint; // swap points
						curPoint = newPoint;

						// 执行UI更新
						//Toast.makeText(activity, "WiFi scan @"+(new Date()), Toast.LENGTH_SHORT).show();
						if (curPoint != null) {
							updateEstCoordText(curPoint.degree);
							viewFloorplan.addTrack(curPoint.px, curPoint.py, curPoint.pAdjX, curPoint.pAdjY, curPoint.radius, curPoint.degree);
							viewFloorplan.invalidate();
						}
					}});
				}
			}
		}, 0, WifiData.WIFI_SCAN_INTERVAL);
	}
	
	public void stopWifiScan() {
		scanTimer.cancel();
		toggleScan.setChecked(false);
	}
	
	public void OnSensorChange(float degree, float gradient) {
		curDirection = degree > 0 ? degree : 360 + degree;
		updateEstCoordText(curDirection);
		if (toggleRotate.isChecked()) {
			viewFloorplan.setCurDirection(curDirection);
			viewFloorplan.invalidate();
		}
	}
}
