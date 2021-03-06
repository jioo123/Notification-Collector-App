package com.example.hyunju.notification_collector;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.Manifest;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.hyunju.notification_collector.models.Contact;

public class MainActivity extends Activity {

    private ListView lv_contactlist;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lv_contactlist = (ListView) findViewById(R.id.lv_contactlist);


        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                ) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS, Manifest.permission.SEND_SMS, Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }else{
            ContactsAdapter adapter = new ContactsAdapter(MainActivity.this,
                    R.layout.layout_phonelist, getContactList());

            lv_contactlist.setAdapter(adapter);
            lv_contactlist
                    .setOnItemClickListener(new AdapterView.OnItemClickListener() {

                        @Override
                        public void onItemClick(AdapterView<?> contactlist, View v,
                                                int position, long resid) {


                            Contact phonenumber = (Contact) contactlist.getItemAtPosition(position);

                            if (phonenumber == null) {
                                return;
                            }


                            Intent intent = new Intent(MainActivity.this, SenderActivity.class);
                            intent.putExtra("phone_num", phonenumber.getPhonenum().replaceAll("-", ""));
                            intent.putExtra("name", phonenumber.getName());
                            intent.putExtra("email", phonenumber.getEmail());

                            startActivity(intent);

                        }
                    });
        }



    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {
            if (grantResults.length > 0) {
                for (int i = 0; i < grantResults.length; ++i) {
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        // 하나라도 거부한다면.
                        new AlertDialog.Builder(this).setTitle("알림").setMessage("권한을 허용해주셔야 앱을 이용할 수 있습니다.")
                                .setPositiveButton("종료", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        MainActivity.this.finish();
                                    }
                                }).setNegativeButton("권한 설정", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                        .setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
                                getApplicationContext().startActivity(intent);
                            }
                        }).setCancelable(false).show();

                        return;
                    }

                }

            }

        }
    }

    @Override
    protected void onResume() {
        super.onResume();

    }


    private ArrayList<Contact> getContactList() {

        ArrayList<Contact> contactlist = new ArrayList<Contact>();

        String[] arrProjection = {
                ContactsContract.Contacts._ID, // ID 열에 해당 하는 정보. 저장된 각 사용자는 고유의 ID를 가진다.
                ContactsContract.Contacts.DISPLAY_NAME // 연락처에 저장된 이름 정보.
        };

        String[] arrPhoneProjection = {
                ContactsContract.CommonDataKinds.Phone.NUMBER // 연락처에 저장된 전화번호 정보
        };

        String[] arrEmailProjection = {
                ContactsContract.CommonDataKinds.Email.DATA // 연락처에 저장된 이메일 정보
        };
        Cursor clsCursor = MainActivity.this.getContentResolver().query(
                ContactsContract.Contacts.CONTENT_URI,
                arrProjection,
                ContactsContract.Contacts.HAS_PHONE_NUMBER + "=1", // HAS_PHONE_NUMBER : 하나 이상의 전화번호가 있으면 1, 그 외에는 0
                null,
                null
        );
        if (clsCursor.moveToFirst()) {
            while (clsCursor.moveToNext()) {
                Contact acontact = new Contact();

                String strContactId = clsCursor.getString(0);

                // Log.d("Unity", "연락처 사용자 ID : " + clsCursor.getString(0));
                Log.d("Unity", "연락처 사용자 이름 : " + clsCursor.getString(1));
                //acontact.setPhotoid(Long.parseLong(clsCursor.getString( 0 )));
                acontact.setName(clsCursor.getString(1));
                // phone number에 접근하는 Cursor
                Cursor clsPhoneCursor = MainActivity.this.getContentResolver().query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        arrPhoneProjection,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + strContactId, // where 절 : 연락처의 ID와 일치하는 전화번호를 가져온다.
                        null,
                        null
                );


                while (clsPhoneCursor.moveToNext()) {
                    //   Log.d("Unity", "연락처 사용자 번호 : " + clsPhoneCursor.getString(0));
                    acontact.setPhonenum(clsPhoneCursor.getString(0));
                }

                clsPhoneCursor.close();


                // email에 접근하는 Cursor
                Cursor clsEmailCursor = MainActivity.this.getContentResolver().query(
                        ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                        arrEmailProjection, // 연락처의 [이메일] 컬럼의 정보를 가져온다.
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + strContactId,
                        null,
                        null
                );


                while (clsEmailCursor.moveToNext()) {
                    Log.d("Unity", "연락처 사용자 email : " + clsEmailCursor.getString(0));
                    acontact.setEmail(clsEmailCursor.getString(0));
                }

                clsEmailCursor.close();


                // note(메모)
                String noteWhere = ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?";
                String[] noteWhereParams = new String[]{
                        strContactId,
                        ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE // MIMETYPE 중 Note(즉, 메모)에 해당하는 내용을 불러오라는 뜻
                };

                // note(메모)에 접근하는 Cursor
                Cursor clsNoteCursor = MainActivity.this.getContentResolver().query(
                        ContactsContract.Data.CONTENT_URI,
                        null,
                        noteWhere,
                        noteWhereParams,
                        null
                );


                while (clsNoteCursor.moveToNext()) {

                    // Log.d("Unity", "연락처 사용자 메모 : " + clsNoteCursor.getString(clsNoteCursor.getColumnIndex(ContactsContract.CommonDataKinds.Note.NOTE)));
                }
                clsNoteCursor.close();


                // address(주소지)
                String addressWhere = ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?";
                String[] addressWhereParams = new String[]{
                        strContactId,
                        ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE  // MIMETYPE 중  StructuredPostal(즉, 우편주소)에 해당하는 내용을 불러오라는 뜻
                };

                Cursor clsAddressCursor = MainActivity.this.getContentResolver().query(
                        ContactsContract.Data.CONTENT_URI,
                        null,
                        addressWhere,
                        addressWhereParams, // addressWhere 첫번째 ?에 addressWhereParams[0]이 들어가고, 두번째 ?d에 addressWhereParams[1]이 들어간다.
                        null
                );


                while (clsAddressCursor.moveToNext()) {
//사용자 주소 쓰고싶으면 이거 활용하면 됨
//                Log.d("Unity", "연락처 사용자 주소 poBox : " + clsAddressCursor.getString(clsAddressCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.POBOX)) );
//                Log.d("Unity", "연락처 사용자 주소 street : " + clsAddressCursor.getString(clsAddressCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.STREET)) );
//                Log.d("Unity", "연락처 사용자 주소 city : " + clsAddressCursor.getString(clsAddressCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.CITY)) );
//                Log.d("Unity", "연락처 사용자 주소 region : " + clsAddressCursor.getString(clsAddressCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.REGION)) );
//                Log.d("Unity", "연락처 사용자 주소 postCode : " + clsAddressCursor.getString(clsAddressCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE)) );
//                Log.d("Unity", "연락처 사용자 주소 country : " + clsAddressCursor.getString(clsAddressCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY)) );
//                Log.d("Unity", "연락처 사용자 주소 type : " + clsAddressCursor.getString(clsAddressCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.TYPE)) );
                }
                // address(주소지) 정보에 접근하는 Cursor 닫는다.
                clsAddressCursor.close();


                // Organization(회사)
                String orgWhere = ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?";
                String[] orgWhereParams = new String[]{
                        strContactId,
                        ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE // MIMETYPE 중  Organization(즉, 회사)에 해당하는 내용을 불러오라는 뜻
                };

                Cursor clsOrgCursor = MainActivity.this.getContentResolver().query(
                        ContactsContract.Data.CONTENT_URI,
                        null,
                        orgWhere,
                        orgWhereParams,
                        null
                );

                while (clsOrgCursor.moveToNext()) {
// 회사/ 직급 활용하고 싶으면 이거 활용
//                Log.d("Unity", "연락처 사용자 회사 : " + clsOrgCursor.getString(clsOrgCursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.DATA)));
//                Log.d("Unity", "연락처 사용자 직급 : " + clsOrgCursor.getString(clsOrgCursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.TITLE)));
                }

                clsOrgCursor.close();
                contactlist.add(acontact);
            }
        }
        clsCursor.close();


        return contactlist;

    }

    private class ContactsAdapter extends ArrayAdapter<Contact> {

        private int resId;
        private ArrayList<Contact> contactlist;
        private LayoutInflater Inflater;
        private Context context;

        public ContactsAdapter(Context context, int textViewResourceId,
                               List<Contact> objects) {
            super(context, textViewResourceId, objects);
            this.context = context;
            resId = textViewResourceId;
            contactlist = (ArrayList<Contact>) objects;
            Inflater = (LayoutInflater) ((Activity) context)
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View v, ViewGroup parent) {
            ViewHolder holder;
            if (v == null) {
                v = Inflater.inflate(resId, null);
                holder = new ViewHolder();
                holder.tv_name = (TextView) v.findViewById(R.id.tv_name);
                holder.tv_phonenumber = (TextView) v
                        .findViewById(R.id.tv_phonenumber);
                holder.iv_photoid = (ImageView) v.findViewById(R.id.iv_photo);
                v.setTag(holder);
            } else {
                holder = (ViewHolder) v.getTag();
            }

            Contact acontact = contactlist.get(position);

            if (acontact != null) {
                holder.tv_name.setText(acontact.getName());
                holder.tv_phonenumber.setText(acontact.getPhonenum());

                Bitmap bm = openPhoto(acontact.getPhotoid());

                if (bm != null) {
                    holder.iv_photoid.setImageBitmap(bm);
                } else {
                    holder.iv_photoid.setImageDrawable(getResources()
                            .getDrawable(R.mipmap.ic_launcher));
                }

            }

            return v;
        }

        private Bitmap openPhoto(long contactId) {
            Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI,
                    contactId);
            InputStream input = Contacts
                    .openContactPhotoInputStream(context.getContentResolver(),
                            contactUri);

            if (input != null) {
                return BitmapFactory.decodeStream(input);
            }

            return null;
        }

        private class ViewHolder {
            ImageView iv_photoid;
            TextView tv_name;
            TextView tv_phonenumber;
        }

    }
    //
}