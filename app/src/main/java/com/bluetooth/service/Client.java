package com.bluetooth.service;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bluetooth.MainActivity;
import com.bluetooth.R;
import com.bluetooth.common.Utils;
import com.bluetooth.common.base.BaseActivity;
import com.tencent.cos.xml.CosXmlService;
import com.tencent.cos.xml.CosXmlServiceConfig;
import com.tencent.cos.xml.exception.CosXmlClientException;
import com.tencent.cos.xml.exception.CosXmlServiceException;
import com.tencent.cos.xml.listener.CosXmlProgressListener;
import com.tencent.cos.xml.listener.CosXmlResultListener;
import com.tencent.cos.xml.model.CosXmlRequest;
import com.tencent.cos.xml.model.CosXmlResult;
import com.tencent.cos.xml.model.bucket.GetBucketRequest;
import com.tencent.cos.xml.model.bucket.GetBucketResult;
import com.tencent.cos.xml.model.bucket.HeadBucketRequest;
import com.tencent.cos.xml.model.bucket.HeadBucketResult;
import com.tencent.cos.xml.model.object.GetObjectRequest;
import com.tencent.cos.xml.model.object.GetObjectResult;
import com.tencent.cos.xml.model.service.GetServiceRequest;
import com.tencent.cos.xml.model.service.GetServiceResult;
import com.tencent.cos.xml.model.tag.ListAllMyBuckets;
import com.tencent.cos.xml.model.tag.ListBucket;
import com.tencent.cos.xml.transfer.COSXMLDownloadTask;
import com.tencent.cos.xml.transfer.TransferConfig;
import com.tencent.cos.xml.transfer.TransferManager;
import com.tencent.cos.xml.transfer.TransferState;
import com.tencent.cos.xml.transfer.TransferStateListener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class Client extends BaseActivity implements View.OnClickListener ,AbsListView.OnScrollListener, ObjectAdapter.OnObjectListener{


    private static final String COS_SECRET_ID = "AKIDwhBPM2QYP0JM8dqmRpwlJaaIkvpRmqd9";
    private static final String COS_SECRET_KEY = "GGzRcW9VgdLo1xc07wVnoXzA5i9JDqCH";
    private static final String COS_BUCKET_NAME = "test-1256011787";
    private CosXmlService cosXmlService;
    private TransferManager transferManager;
    private COSXMLDownloadTask cosxmlTask;
    private File downloadParentDir;
    List<String> fileName;
    //?????????
    private TextView tv_name;
    //????????????
    private TextView tv_state;
    //????????????
    private TextView tv_progress;
    //??????????????????
    private TextView tv_path;
    //???????????????
    private ProgressBar pb_download;

    //?????????????????????????????????
    private Button btn_left;
    //?????????????????????????????????
    private Button btn_right;
    private Button fanhui;

    String PrinterName;
    String FrName;
    private ProgressDialog progressDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.download_activity);
        tv_name = findViewById(R.id.tv_name);
        tv_state = findViewById(R.id.tv_state);
        tv_progress = findViewById(R.id.tv_progress);
        tv_path = findViewById(R.id.tv_path);
        pb_download = findViewById(R.id.pb_download);
        btn_left = findViewById(R.id.btn_left);
        btn_right = findViewById(R.id.btn_right);
        fanhui = findViewById(R.id.fanhui);

        btn_right.setOnClickListener(this);
        btn_left.setOnClickListener(this);
        fanhui.setOnClickListener(this);
        fanhui.setVisibility(View.GONE);

        fileName=new ArrayList<>();


        PrinterName = getIntent().getExtras().getString("PrinterName");

        onCreate();




        progressDialog = ProgressDialog.show(Client.this,"Loading...", "Please wait...", true, false);
        progressDialog.setCancelable(false);


//??????????????????CustomProgressDialog

    }
    void onCreate(){

        cosXmlService = CosServiceFactory.getCosXmlService(this, COS_SECRET_ID, COS_SECRET_KEY, false);
        TransferConfig transferConfig = new TransferConfig.Builder().build();
        transferManager = new TransferManager(cosXmlService, transferConfig);
        downloadParentDir = getExternalFilesDir("/");
        getObject();
    }

    String findNetFirmware(String printerName)
    {
        String newprint = printerName.split("_")[0];
        String FName=null;
        if (fileName.size()<=0)
            return null;
        for (int i=0;i<fileName.size();i++)
        {
            String newfir = fileName.get(i).split("-")[0];
            if (newfir.equals(newprint)){
                return fileName.get(i);
            }

        }
        return FName;
    }


    void getObject()
    {
        final GetBucketRequest getBucketRequest = new GetBucketRequest(COS_BUCKET_NAME);

        // ??????????????????????????????????????????1000
        getBucketRequest.setMaxKeys(100);

        // ???????????????????????????????????? Prefix???
        // ?????? Prefix ??? delimiter ????????????????????????????????????????????? Common Prefix???
        // ?????????????????? Common Prefix??????????????? Prefix???????????????????????????
        getBucketRequest.setDelimiter("/");

        // ????????????????????????
        cosXmlService.getBucketAsync(getBucketRequest, new CosXmlResultListener() {
            @Override
            public void onSuccess(CosXmlRequest request, CosXmlResult result) {
                final GetBucketResult getBucketResult = (GetBucketResult) result;
                uiAction(new Runnable() {
                    @Override
                    public void run() {
                        if (getBucketResult.listBucket.contentsList.size()>0)
                        {
                            List<ListBucket.Contents> temp = getBucketResult.listBucket.contentsList;
                            for (int i=0;i<temp.size();i++)
                            {
                                fileName.add(temp.get(i).key);
                            }
                        }
                        FrName = findNetFirmware(PrinterName);
                        progressDialog.dismiss();

                    }
                });
            }

            @Override
            public void onFail(CosXmlRequest cosXmlRequest, CosXmlClientException clientException, CosXmlServiceException serviceException) {
                //??????????????????loading  ???????????????loading
                clientException.printStackTrace();
                serviceException.printStackTrace();
            }
        });
    }


    /**
     * ??????????????????
     * @param state ?????? {@link TransferState}
     */
    private void refreshState(final TransferState state) {
        uiAction(new Runnable() {
            @Override
            public void run() {
                tv_state.setText(state.toString());
            }
        });
    }

    /**
     * ??????????????????
     * @param progress ?????????????????????
     * @param total ???????????????
     */
    private void refreshProgress(final long progress, final long total) {
        uiAction(new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                pb_download.setProgress((int) (100 * progress / total));
                tv_progress.setText(Utils.readableStorageSize(progress) + "/" + Utils.readableStorageSize(total));
            }
        });
    }

    private void download(final String urlpath) {

        if (urlpath==null)
        {
            toastMessage("?????????");
            return;
        }
        tv_name.setText("???????????????" + urlpath);
        if (cosxmlTask == null) {
            cosxmlTask = transferManager.download(this, COS_BUCKET_NAME, urlpath,
                    downloadParentDir.toString(), urlpath);

            cosxmlTask.setTransferStateListener(new TransferStateListener() {
                @Override
                public void onStateChanged(final TransferState state) {
                    refreshState(state);
                }
            });

            cosxmlTask.setCosXmlProgressListener(new CosXmlProgressListener() {
                @Override
                public void onProgress(final long complete, final long target) {
                    refreshProgress(complete, target);
                }
            });

            cosxmlTask.setCosXmlResultListener(new CosXmlResultListener() {
                @SuppressLint("SetTextI18n")
                @Override
                public void onSuccess(CosXmlRequest request, CosXmlResult result) {
                    cosxmlTask = null;
                    toastMessage("????????????");
                    uiAction(new Runnable() {
                        @Override
                        public void run() {
                            btn_left.setVisibility(View.GONE);
                            btn_right.setVisibility(View.GONE);
                            fanhui.setVisibility(View.VISIBLE);
                            tv_path.setText("?????????????????????" + downloadParentDir.toString() + "/" + urlpath);

                        }
                    });
                }

                @Override
                public void onFail(CosXmlRequest request, CosXmlClientException exception, CosXmlServiceException serviceException) {
                    if (cosxmlTask.getTaskState() != TransferState.PAUSED) {
                        cosxmlTask = null;
                        toastMessage("????????????");
                        uiAction(new Runnable() {
                            @Override
                            public void run() {
                                pb_download.setProgress(0);
                                tv_progress.setText("");
                                tv_state.setText("???");
                                btn_left.setText("??????");
                            }
                        });
                    }

                    if (exception != null) {
                        exception.printStackTrace();
                    }
                    if (serviceException != null) {
                        serviceException.printStackTrace();
                    }
                }
            });
            btn_left.setText("??????");
        }
    }


    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_left) {
            if ("??????".contentEquals(btn_left.getText())) {
                download(FrName);
            } else if("??????".contentEquals(btn_left.getText())) {//??????
                if (cosxmlTask != null) {
                    cosxmlTask.cancel();
                    finish();
                } else {
                    toastMessage("????????????");
                }
            }
        } else if (v.getId() == R.id.btn_right) {
            if ("??????".contentEquals(btn_right.getText())) {
                if (cosxmlTask != null && cosxmlTask.getTaskState() == TransferState.IN_PROGRESS) {
                    cosxmlTask.pause();
                    btn_right.setText("??????");
                } else {
                    toastMessage("????????????");
                }
            } else {//??????
                if (cosxmlTask != null && cosxmlTask.getTaskState() == TransferState.PAUSED) {
                    cosxmlTask.resume();
                    btn_right.setText("??????");
                } else {
                    toastMessage("????????????");
                }
            }
        }else if (v.getId() == R.id.fanhui)
        {
            Intent i = new Intent(Client.this, MainActivity.class);
            startActivity(i);
            return;
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {

    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

    }

    @Override
    public void onFolderClick(String prefix) {

    }

    @Override
    public void onDownload(ObjectEntity object) {

    }

    @Override
    public void onDelete(ObjectEntity object) {

    }
}
