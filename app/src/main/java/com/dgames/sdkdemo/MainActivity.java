package com.dgames.sdkdemo;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.dgames.sdk.DGameManager;
import com.dgames.sdk.ILoginCallBack;
import com.dgames.sdk.IPayCallback;
import com.dgames.sdk.ITransdErcCallBack;
import com.dgames.sdk.PermissionListener;
import com.dgames.sdk.bean.Config;
import com.dgames.sdk.utils.FloatGravity;
import com.dgames.sdk.utils.PermissionUtils;
import com.dgames.sdk.utils.ToastFactory;
import com.dgames.sdk.wallet.ICallBack;
import com.dgames.sdk.wallet.WalletManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class MainActivity extends Activity implements View.OnClickListener {
    private Button btn_center, btn_login, btn_pay, btn_query, btn_asset, btn_query_game, btn_query_dgas;
    private EditText edit;
    private String[] perms;
    private String appId = "V431rSWOMpq3xGGJYSQTGH5oxlMBiXjJRw";
    //star  appid
//    private String appId = "VxIlXUGPLo-XmtB1COUHnUHEYePEBXNXXA";
    private String export_browse = "http://192.168.60.16:801";
    private String decimals = "100000000";
    private String language = "cn";
    //erc token id
    private String chainId = "458";
    private String tokenId = "458";
    public Config config;
    private String orderId = null;
    private PermissionUtils permissionUtils;
    private static final int REQUEST_CODE_SDK_RESULT_PERMISSIONS = 102;
    private boolean showRequestPermission = false;
    private String address = "";
    //Game equipment information
    private String equip_info;
    private String id = "123456789";
    private String jpg = "https://ss0.baidu.com/73t1bjeh1BF3odCf/it/u=3235477355,1640820814&fm=85&s=BE921DCB2761011152E0B0370300C050";
    private String name = "person";
    private String price = "100";
    private String remarks = "jksgihjsmbhgtswjhjm";
    //equip id name time
    private String comment = "123,person," + System.currentTimeMillis();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        edit = findViewById(R.id.edit);
        btn_center = (Button) findViewById(R.id.btn_center);
        btn_login = (Button) findViewById(R.id.btn_login);
        btn_pay = (Button) findViewById(R.id.btn_pay);
        btn_query = (Button) findViewById(R.id.btn_query);
        btn_asset = (Button) findViewById(R.id.btn_asset);
        btn_query_game = findViewById(R.id.btn_query_game);
        btn_query_dgas = findViewById(R.id.btn_query_dgas);
        btn_center.setOnClickListener(this);
        btn_login.setOnClickListener(this);
        btn_pay.setOnClickListener(this);
        btn_query.setOnClickListener(this);
        btn_asset.setOnClickListener(this);
        btn_query_game.setOnClickListener(this);
        btn_query_dgas.setOnClickListener(this);

        getConfig();
        perms = new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.ACCESS_FINE_LOCATION,
        };
        //Permission Request
        permissionUtils = new PermissionUtils(this);
        setPermission();
        equip_info = getEquipInfo(id, jpg, name, price, remarks);
        DGameManager.setSDKLoginCallback(new ILoginCallBack() {
            @Override
            public void OnSuccess(String message) {
                try {
                    JSONObject object = new JSONObject(message);
                    //Login validation callback
                    String signdata = object.getString("signdata");
                    String uname = object.getString("uname");
                    address = object.getString("address");

                    ToastFactory.showToast(MainActivity.this, signdata);
                    Log.e("++++signdata++++++", message);
                    //Display suspension ball
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void OnFailed(String msg) {
                ToastFactory.showToast(MainActivity.this, msg);
            }
        });
    }

    private String getEquipInfo(String id, String jpg, String name, String price, String remarks) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("id", id);
            obj.put("jpg", jpg);
            obj.put("name", name);
            obj.put("price", price);
            obj.put("remarks", remarks);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj.toString();

    }


    private void setPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            permissionUtils.requestRunPermisssion(perms, new PermissionListener() {
                @Override
                public void onGranted() {
                    //he instructions are all authorized
                    DGameManager.init(MainActivity.this, config);
                }

                @Override
                public void onDenied(List<String> deniedPermission) {
                    for (int i = 0; i < deniedPermission.size(); i++) {
                        showRequestPermission = ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, deniedPermission.get(i));
                    }
                    if (showRequestPermission) {
                        setPermission();
                    } else {
                        showMissingPermissionDialog();
                    }
                }
            });
        } else {
            DGameManager.init(MainActivity.this, config);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionUtils.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    //Need to manually open the missing permissions dialog
    private void showMissingPermissionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("prompt");
        builder.setMessage("For your normal use of SDK, please open the permissions!");
        builder.setPositiveButton("Go Set", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startAppSettings();
            }
        });
        builder.setCancelable(false);
        builder.show();
    }

    //Start application settings to manually open permissions
    private void startAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, REQUEST_CODE_SDK_RESULT_PERMISSIONS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SDK_RESULT_PERMISSIONS) {
            setPermission();
        }
    }

    public void getConfig() {
        config = new Config();
        //sdk appId
        config.setAppId(appId);
        //Suspension ball initialization display position
        config.setFloatGravity(FloatGravity.TOP_CENTER);
        //Browser address
        config.setApibrowse(export_browse);
        //double-precision value
        config.setDecimals(decimals);
        //Screen vertical screen setting,Horizontal screen is true
        config.setLandscape(false);
        //Is URL a test version or a formal version for dgame
        config.setDgameDebug(true);
        //Set project language
        config.setLanguage(language);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.btn_login:
                login();
                break;
            case R.id.btn_center:
                openUserCenter();
                break;
            case R.id.btn_pay:
                pay();
                break;
            case R.id.btn_query:
                queryErc();
                break;
            case R.id.btn_asset:
                transLateErc();
                break;
            case R.id.btn_query_game:
                queryGameAmount(address);
                break;
            case R.id.btn_query_dgas:
                queryDgasAmount(address);
                break;
            default:
                break;
        }
    }

    //Query account subchain balance
    private void queryGameAmount(String address) {
        if (DGameManager.isLogin() == Config.LOGIN) {
            String str = DGameManager.queryGameAmount(address);
            ToastFactory.showToast(MainActivity.this, "The balance of the user's sub chain currency is==" + str);
        } else {
            login();
        }
    }

    //Query account DGAs balance
    private void queryDgasAmount(String address) {
        if (DGameManager.isLogin() == Config.LOGIN) {
            DGameManager.queryDgasAmount(address, new ICallBack() {
                @Override
                public void invoke(String str) {
                    ToastFactory.showToast(MainActivity.this, "The DGAs balance of the user is==" + str);
                }
            });
        } else {
            login();
        }

    }

    private void transLateErc() {
//        WalletManager.getInstance().gameTranscationAmount("dtbdtt", "V8rl1mjzPKeztD2CbvaS0SRd9-k6j0Zzyw", tokenId, comment_erc, new ICallBack() {
//            @Override
//            public void invoke(String str) {
//                ToastFactory.showToast(MainActivity.this, str);
//            }
//        });
        if (DGameManager.isLogin() == Config.LOGIN) {
            orderId = String.valueOf(Math.random() * 1000000000);
            if (tokenId == null || tokenId.equals("")) {
                ToastFactory.showToast(this, "Please enter erc TokenID!");
            } else {
                DGameManager.setSDKsetSDKTransErcCallback(new ITransdErcCallBack() {
                    @Override
                    public void onTransErcSuccess(String successStr) {
                        ToastFactory.showToast(MainActivity.this, successStr);
                    }

                    @Override
                    public void onTransErcFail(String failedStr) {
                        ToastFactory.showToast(MainActivity.this, failedStr);
                    }
                });
                DGameManager.transErc(MainActivity.this, orderId, tokenId, equip_info,comment);
            }
        } else {
            login();
        }
    }

    private void queryErc() {
        WalletManager.getInstance().gameQueryAssetErc(chainId, new ICallBack() {
            @Override
            public void invoke(String str) {
                ToastFactory.showToast(MainActivity.this, str);
            }
        });
    }

    public void login() {
        DGameManager.login(MainActivity.this);
    }

    public void openUserCenter() {
        if (DGameManager.isLogin() == Config.LOGIN) {
            DGameManager.openUserCenter();
        } else {
            login();
        }
    }

    public void pay() {
        if (DGameManager.isLogin() == Config.LOGIN) {
            orderId = String.valueOf(Math.random() * 1000000000);
            String amount = edit.getText().toString().trim();
            if (amount == null || amount.equals("")) {
                ToastFactory.showToast(this, "Please enter amount!");
            } else if (amount.equals("0")) {
                ToastFactory.showToast(this, "amount can not be 0,Please enter amount!");
            } else {
                DGameManager.setSDKPayCallback(new IPayCallback() {
                    @Override
                    public void onPaySuccess(String jsonStr) {
                        try {
                            JSONObject object = new JSONObject(jsonStr);
                            //Payment order number
                            String payOrderId = object.getString("payOrderId");
                            //Transaction Number
                            String txid = object.getString("txid");

                            ToastFactory.showToast(MainActivity.this, jsonStr.toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onPayFail(String error) {
                        ToastFactory.showToast(MainActivity.this, error);
                    }
                });
                DGameManager.pay(MainActivity.this, orderId, amount, System.currentTimeMillis() + "");
            }
        } else {
            login();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        DGameManager.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        DGameManager.showFloatingView();
        if (DGameManager.isLogin() == Config.LOGINOUT) {
            ToastFactory.showToast(MainActivity.this, "The game has quit logon");
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        DGameManager.hideFloatingView();
    }

    @Override
    protected void onStop() {
        super.onStop();
        DGameManager.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DGameManager.onDestory();
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Are you sure you want to quit the game?")
                .setPositiveButton("sure", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MainActivity.super.onBackPressed();
                    }
                })
                .setNegativeButton("cancel", null)
                .show();
    }
}