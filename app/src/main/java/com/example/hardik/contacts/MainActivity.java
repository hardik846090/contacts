package com.example.hardik.contacts;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    Button mContactload, mContactadd, mContactupdate, mContactdelete;
    private static final String TAG_ANDROID_CONTACTS = "=====>>";
    private static final int CONTACTS_LOADER_ID = 1;
    private int id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //getLoaderManager().initLoader(CONTACTS_LOADER_ID, null, (android.app.LoaderManager.LoaderCallbacks<Object>) this);

        mContactload = findViewById(R.id.contactload);
        mContactadd = findViewById(R.id.contactadd);
        mContactupdate = findViewById(R.id.contactupdate);
        mContactdelete = findViewById(R.id.contactdelete);

        setTitle("Contacts");

        // Load all contacts, and print each contact as log debug info.
        Button loadButton = (Button) findViewById(R.id.contactload);
        mContactload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!hasPhoneContactsPermission(Manifest.permission.READ_CONTACTS)) {
                    requestPermission(Manifest.permission.READ_CONTACTS);
                } else {
                    getAllContacts();
                    Toast.makeText(MainActivity.this, "Contact data has been printed in the android monitor log..", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mContactadd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!hasPhoneContactsPermission(Manifest.permission.WRITE_CONTACTS)) {
                    requestPermission(Manifest.permission.WRITE_CONTACTS);
                } else {
                    addContact();
                    Toast.makeText(MainActivity.this, "Contact added", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Create a new contact and add it to android contact address book.

    private void addContact() {

        // Create a new fake contact first.
        ContactDTO contactDto = generateRandomContactDTO(-1);

        // First get contentresolver object.
        ContentResolver contentResolver = getContentResolver();

        String groupTitle = "Workmate";

        String groupNotes = "Company workmates contacts";

        long groupId = getExistGroup(contentResolver, groupTitle);

        // Group do not exist.
        if (groupId == -1) {
            // Create a new group
            groupId = insertGroup(contentResolver, groupTitle, groupNotes);
        }

        // Create a new contact.
        long rawContactId = insertContact(contentResolver, contactDto);

        // Set group id.
        contactDto.setGroupId(groupId);
        // Contact id and raw contact id has same value.
        contactDto.setContactId(rawContactId);
        contactDto.setRawContactId(rawContactId);

        // Insert contact group membership data ( group id ).
        insertGroupId(contentResolver, contactDto.getGroupId(), contactDto.getRawContactId());

        // Insert contact address list data.
        insertListData(contentResolver, ContactsContract.Data.CONTENT_URI,
                contactDto.getRawContactId(),
                ContactsContract.CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE,
                ContactsContract.CommonDataKinds.SipAddress.TYPE,
                ContactsContract.CommonDataKinds.SipAddress.SIP_ADDRESS,
                contactDto.getAddressList());

        // Insert organization data.
        insertOrganization(contentResolver, contactDto);

        // Insert contact display, given and family name.
        insertName(contentResolver, contactDto);

        /* Insert contact email list data, Content uri do not use ContactsContract.CommonDataKinds.Email.CONTENT_URI
         * Otherwise it will throw error java.lang.UnsupportedOperationException: URI: content://com.android.contacts/data/emails */
        insertListData(contentResolver, ContactsContract.Data.CONTENT_URI,
                contactDto.getRawContactId(),
                ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE,
                ContactsContract.CommonDataKinds.Email.TYPE,
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                contactDto.getEmailList());

        // Insert contact nickname.
        insertNickName(contentResolver, contactDto);

        // Insert contact note.
        insertNote(contentResolver, contactDto);

        /* Insert contact phone list data, Content uri do not use ContactsContract.CommonDataKinds.Phone.CONTENT_URI
         * Otherwise it will throw error java.lang.UnsupportedOperationException: URI: content://com.android.contacts/data/phones */
        insertListData(contentResolver, ContactsContract.Data.CONTENT_URI,
                contactDto.getRawContactId(),
                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
                ContactsContract.CommonDataKinds.Phone.TYPE,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                contactDto.getPhoneList());

        // Insert contact website list.
        insertListData(contentResolver, ContactsContract.Data.CONTENT_URI,
                contactDto.getRawContactId(),
                ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE,
                ContactsContract.CommonDataKinds.Website.TYPE,
                ContactsContract.CommonDataKinds.Website.URL,
                contactDto.getWebsiteList());

        // Insert contact im list.
        insertListData(contentResolver, ContactsContract.Data.CONTENT_URI,
                contactDto.getRawContactId(),
                ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE,
                ContactsContract.CommonDataKinds.Im.PROTOCOL,
                ContactsContract.CommonDataKinds.Im.DATA,
                contactDto.getImList());

        // Insert contact post address
        insertPostalAddress(contentResolver, contactDto);

        // Insert identity
        insertIdentity(contentResolver, contactDto);

        // Insert photo
        insertPhoto(contentResolver, contactDto);

    }

    private void insertOrganization(ContentResolver contentResolver, ContactDTO contactDto) {
        if(contactDto!=null) {

            ContentValues contentValues = new ContentValues();

            // Set raw contact id. Data table only has raw_contact_id.
            contentValues.put(ContactsContract.Data.RAW_CONTACT_ID, contactDto.getRawContactId());
            // Set data mimetype.
            contentValues.put(ContactsContract.CommonDataKinds.Organization.MIMETYPE, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE);
            // Set company name.
            contentValues.put(ContactsContract.CommonDataKinds.Organization.COMPANY, contactDto.getCompany());
            // Set department.
            contentValues.put(ContactsContract.CommonDataKinds.Organization.DEPARTMENT, contactDto.getDepartment());
            // Set title.
            contentValues.put(ContactsContract.CommonDataKinds.Organization.TITLE, contactDto.getTitle());
            // Set job description.
            contentValues.put(ContactsContract.CommonDataKinds.Organization.JOB_DESCRIPTION, contactDto.getJobDescription());
            // Set office location.
            contentValues.put(ContactsContract.CommonDataKinds.Organization.OFFICE_LOCATION, contactDto.getOfficeLocation());

            // Insert to data table.
            contentResolver.insert(ContactsContract.Data.CONTENT_URI, contentValues);
        }

    }

    private void insertGroupId(ContentResolver contentResolver, long groupId, long rawContactId) {


        ContentValues contentValues = new ContentValues();
        // Set raw contact id. Data table only has raw_contact_id.
        contentValues.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);

        // Set mimetype first.
        contentValues.put(ContactsContract.CommonDataKinds.StructuredName.MIMETYPE, ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE);
        // Set contact belongs group id.
        byte[] groupRowId = new byte[0];
        contentValues.put(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID, groupRowId);

        // Insert to data table.

    }

    private void insertNickName(ContentResolver contentResolver, ContactDTO contactDto) {

        if(contactDto!=null) {

            ContentValues contentValues = new ContentValues();

            // Set raw contact id. Data table only has raw_contact_id.
            contentValues.put(ContactsContract.Data.RAW_CONTACT_ID, contactDto.getRawContactId());
            // Set data mimetype.
            contentValues.put(ContactsContract.CommonDataKinds.Nickname.MIMETYPE, ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE);
            // Set display name.
            contentValues.put(ContactsContract.CommonDataKinds.Nickname.NAME, contactDto.getNickName());
            // Insert to data table.
            contentResolver.insert(ContactsContract.Data.CONTENT_URI, contentValues);
        }

    }

    private long insertContact(ContentResolver contentResolver, ContactDTO contactDto) {
        // Insert an empty contact in both contacts and raw_contacts table.
        // Return the system generated new contact and raw_contact id.
        // The id in contacts and raw_contacts table has same value.
        ContentValues contentValues = new ContentValues();
        contentValues.put(ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY, contactDto.getDisplayName());
        contentValues.put(ContactsContract.RawContacts.DISPLAY_NAME_ALTERNATIVE, contactDto.getDisplayName());

        Uri rawContactUri = contentResolver.insert(ContactsContract.RawContacts.CONTENT_URI, contentValues);
        // Get the newly created raw contact id.
        long rawContactId = ContentUris.parseId(rawContactUri);
        return rawContactId;
    }

    private void insertNote(ContentResolver contentResolver, ContactDTO contactDto) {
        if(contactDto!=null) {

            ContentValues contentValues = new ContentValues();

            // Set raw contact id. Data table only has raw_contact_id.
            contentValues.put(ContactsContract.Data.RAW_CONTACT_ID, contactDto.getRawContactId());
            // Set data mimetype.
            contentValues.put(ContactsContract.CommonDataKinds.Note.MIMETYPE, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE);
            // Set display name.
            contentValues.put(ContactsContract.CommonDataKinds.Note.NOTE, contactDto.getNote());
            // Insert to data table.
            contentResolver.insert(ContactsContract.Data.CONTENT_URI, contentValues);
        }

    }

    private void insertName(ContentResolver contentResolver, ContactDTO contactDto) {
        if(contactDto!=null) {

            ContentValues contentValues = new ContentValues();

            // Set raw contact id. Data table only has raw_contact_id.
            contentValues.put(ContactsContract.Data.RAW_CONTACT_ID, contactDto.getRawContactId());
            // Set data mimetype.
            contentValues.put(ContactsContract.CommonDataKinds.StructuredName.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
            // Set display name.
            contentValues.put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contactDto.getDisplayName());
            // Set given name.
            contentValues.put(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, contactDto.getGivenName());
            // Set family name.
            contentValues.put(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, contactDto.getFamilyName());
            // Insert to data table.
            contentResolver.insert(ContactsContract.Data.CONTENT_URI, contentValues);
        }


    }

    private void insertListData(ContentResolver contentResolver, Uri contentUri, long rawContactId, String contentItemType, String protocol, String data, List<DataDTO> imList) {
        // Insert contact address list data.
        ContactDTO contactDto = null;
        insertListData(contentResolver, ContactsContract.Data.CONTENT_URI,
                contactDto.getRawContactId(),
                ContactsContract.CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE,
                ContactsContract.CommonDataKinds.SipAddress.TYPE,
                ContactsContract.CommonDataKinds.SipAddress.SIP_ADDRESS,
                contactDto.getAddressList());

    }

    private void insertPhoto(ContentResolver contentResolver, ContactDTO contactDto) {
        ContentValues contentValues = new ContentValues();
        // Set raw contact id. Data table only has raw_contact_id.
        contentValues.put(ContactsContract.Data.RAW_CONTACT_ID, contactDto.getRawContactId());
        // Set mimetype first.
        contentValues.put(ContactsContract.CommonDataKinds.Photo.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE);
        // Set photo
        contentValues.put(ContactsContract.CommonDataKinds.Photo.PHOTO, contactDto.getPhoto());
        // Set photo file id.
        contentValues.put(ContactsContract.CommonDataKinds.Photo.PHOTO_FILE_ID, contactDto.getPhotoFieldId());

        // Insert to data table.
        contentResolver.insert(ContactsContract.Data.CONTENT_URI, contentValues);

    }

    private void insertIdentity(ContentResolver contentResolver, ContactDTO contactDto) {
        ContentValues contentValues = new ContentValues();
        // Set raw contact id. Data table only has raw_contact_id.
        contentValues.put(ContactsContract.Data.RAW_CONTACT_ID, contactDto.getRawContactId());
        // Set mimetype first.
        contentValues.put(ContactsContract.CommonDataKinds.Identity.MIMETYPE, ContactsContract.CommonDataKinds.Identity.CONTENT_ITEM_TYPE);
        // Set identity
        contentValues.put(ContactsContract.CommonDataKinds.Identity.IDENTITY, contactDto.getIdentity());
        // Set namespace
        contentValues.put(ContactsContract.CommonDataKinds.Identity.NAMESPACE, contactDto.getNamespace());

        // Insert to data table.
        contentResolver.insert(ContactsContract.Data.CONTENT_URI, contentValues);

    }

    private void insertPostalAddress(ContentResolver contentResolver, ContactDTO contactDto) {
        ContentValues contentValues = new ContentValues();
        // Set raw contact id. Data table only has raw_contact_id.
        contentValues.put(ContactsContract.Data.RAW_CONTACT_ID, contactDto.getRawContactId());
        // Set mimetype first.
        contentValues.put(ContactsContract.CommonDataKinds.StructuredPostal.MIMETYPE, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE);
        // Set country
        contentValues.put(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY, contactDto.getCountry());
        // Set city
        contentValues.put(ContactsContract.CommonDataKinds.StructuredPostal.CITY, contactDto.getCity());
        // Set region
        contentValues.put(ContactsContract.CommonDataKinds.StructuredPostal.REGION, contactDto.getRegion());
        // Set street
        contentValues.put(ContactsContract.CommonDataKinds.StructuredPostal.STREET, contactDto.getStreet());
        // Set postcode
        contentValues.put(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE, contactDto.getPostCode());
        // Set postcode
        contentValues.put(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, contactDto.getPostType());

    /* Insert to data table. Do not use uri ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI,
       Otherwise it will throw error java.lang.UnsupportedOperationException: URI: content://com.android.contacts/data/postals*/
        contentResolver.insert(ContactsContract.Data.CONTENT_URI, contentValues);


    }

    private long insertGroup(ContentResolver contentResolver, String groupTitle, String groupNotes) {

        // Insert a group in group table.
        ContentValues contentValues = new ContentValues();
        contentValues.put(ContactsContract.Groups.TITLE, groupTitle);
        contentValues.put(ContactsContract.Groups.NOTES, groupNotes);
        Uri groupUri = contentResolver.insert(ContactsContract.Groups.CONTENT_URI, contentValues);
        // Get the newly created raw contact id.
        long groupId = ContentUris.parseId(groupUri);

        return groupId;
    }

    private long getExistGroup(ContentResolver contentResolver, String groupTitle) {
        long ret = -1;

        String queryColumnArr[] = {ContactsContract.Groups._ID};

        StringBuffer whereClauseBuf = new StringBuffer();
        whereClauseBuf.append(ContactsContract.Groups.TITLE);
        whereClauseBuf.append("='");
        whereClauseBuf.append(groupTitle);
        whereClauseBuf.append("'");

        Cursor cursor = contentResolver.query(ContactsContract.Groups.CONTENT_URI, queryColumnArr, whereClauseBuf.toString(), null, null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(ContactsContract.Groups._ID);
                ret = cursor.getLong(columnIndex);
            }
        }
        return ret;
    }

    private ContactDTO generateRandomContactDTO(long currentSystemTime) {

        ContactDTO contactDto = new ContactDTO();

        String uuidStr = "";

        if (currentSystemTime != -1) {
            uuidStr += "_" + currentSystemTime;
        }

        //**************************************************************
        // Create contact address list.
        List<DataDTO> addressList = new ArrayList<DataDTO>();

        // Create home address.
        DataDTO homeAddressDto = new DataDTO();
        homeAddressDto.setDataType(ContactsContract.CommonDataKinds.SipAddress.TYPE_HOME);
        homeAddressDto.setDataValue("3122 Camden Street" + uuidStr);
        addressList.add(homeAddressDto);

        // Create work address.
        DataDTO workAddressDto = new DataDTO();
        workAddressDto.setDataType(ContactsContract.CommonDataKinds.SipAddress.TYPE_WORK);
        workAddressDto.setDataValue("3819 Watson Street" + uuidStr);
        addressList.add(workAddressDto);

        // Add address list.
        contactDto.setAddressList(addressList);

        //***************************************************************
        // Below is contact organization related info.

        // Set company
        contactDto.setCompany("IBM" + uuidStr);
        // Set department
        contactDto.setDepartment("Development Team" + uuidStr);
        // Set title
        contactDto.setTitle("Senior Software Engineer" + uuidStr);
        // Set job description
        contactDto.setJobDescription("Develop features use java." + uuidStr);
        // Set office location.
        contactDto.setOfficeLocation("Mountain View" + uuidStr);

        //***************************************************************
        // Create email address list.
        List<DataDTO> emailList = new ArrayList<DataDTO>();

        // Create work email.
        DataDTO workEmailDto = new DataDTO();
        workEmailDto.setDataType(ContactsContract.CommonDataKinds.Email.TYPE_WORK);
        workEmailDto.setDataValue("jack" + uuidStr + "@dev2qa.com");
        emailList.add(workEmailDto);

        // Create home email.
        DataDTO homeEmailDto = new DataDTO();
        homeEmailDto.setDataType(ContactsContract.CommonDataKinds.Email.TYPE_HOME);
        homeEmailDto.setDataValue("jack" + uuidStr + "@gmail.com");
        emailList.add(homeEmailDto);

        // Add email list.
        contactDto.setEmailList(emailList);

        //***************************************************************
        // Below is structured name related info.

        contactDto.setDisplayName("Jack" + uuidStr);

        contactDto.setGivenName("Bill" + uuidStr);

        contactDto.setFamilyName("Trump" + uuidStr);

        //**************************************************************
        // Contact nick name related info.

        contactDto.setNickName("FlashMan" + uuidStr);

        //**************************************************************
        // Contact note related info.
        contactDto.setNote("dev2qa.com senior engineer" + uuidStr);

        //**************************************************************
        // Im related info
        List<DataDTO> imList = new ArrayList<DataDTO>();

        DataDTO qqDto = new DataDTO();
        qqDto.setDataType(ContactsContract.CommonDataKinds.Im.PROTOCOL_QQ);
        qqDto.setDataValue("QQ_888" + uuidStr);
        imList.add(qqDto);

        DataDTO icqDto = new DataDTO();
        icqDto.setDataType(ContactsContract.CommonDataKinds.Im.PROTOCOL_ICQ);
        icqDto.setDataValue("ICQ_666" + uuidStr);
        imList.add(icqDto);

        DataDTO skypeDto = new DataDTO();
        skypeDto.setDataType(ContactsContract.CommonDataKinds.Im.PROTOCOL_SKYPE);
        skypeDto.setDataValue("SKYPE_968" + uuidStr);
        imList.add(skypeDto);

        contactDto.setImList(imList);

        //***************************************************************
        // Create phone list.
        List<DataDTO> phoneList = new ArrayList<DataDTO>();

        // Create mobile phone.
        DataDTO mobilePhone = new DataDTO();
        mobilePhone.setDataType(ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE);
        mobilePhone.setDataValue("13901111118" + uuidStr);
        phoneList.add(mobilePhone);

        // Create work mobile phone.
        DataDTO workMobilePhone = new DataDTO();
        workMobilePhone.setDataType(ContactsContract.CommonDataKinds.Phone.TYPE_WORK_MOBILE);
        workMobilePhone.setDataValue("13801234567" + uuidStr);
        phoneList.add(workMobilePhone);

        // Create home phone.
        DataDTO homePhone = new DataDTO();
        homePhone.setDataType(ContactsContract.CommonDataKinds.Phone.TYPE_HOME);
        homePhone.setDataValue("010-123456789" + uuidStr);
        phoneList.add(homePhone);

        contactDto.setPhoneList(phoneList);

        //***************************************************************
        // Create website list
        List<DataDTO> websiteList = new ArrayList<DataDTO>();

        // Create work website dto.
        DataDTO workWebsiteDto = new DataDTO();
        workWebsiteDto.setDataType(ContactsContract.CommonDataKinds.Website.TYPE_WORK);
        workWebsiteDto.setDataValue(uuidStr + ".dev2qa.com");
        websiteList.add(workWebsiteDto);

        // Create blog website dto.
        DataDTO blogWebsiteDto = new DataDTO();
        blogWebsiteDto.setDataType(ContactsContract.CommonDataKinds.Website.TYPE_BLOG);
        blogWebsiteDto.setDataValue(uuidStr + ".blog.dev2qa.com");
        websiteList.add(blogWebsiteDto);

        contactDto.setWebsiteList(websiteList);

        //**************************************************************
        // Set postal related info.
        contactDto.setCountry("USA" + uuidStr);
        contactDto.setCity("Chicago" + uuidStr);
        contactDto.setRegion("Washington DC" + uuidStr);
        contactDto.setStreet("2260 West Drive" + uuidStr);
        contactDto.setPostCode("60606" + uuidStr);
        contactDto.setPostType(ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME);

        //**************************************************************
        // Set identity info.
        contactDto.setIdentity("347-80-XXXX" + uuidStr);
        contactDto.setNamespace("SSN");

        //**************************************************************
        // Set photo info.
        contactDto.setPhoto(uuidStr + ".png");
        contactDto.setPhotoFieldId(uuidStr);

        return contactDto;

    }

    /*mContactload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!hasPhoneContactsPermission(Manifest.permission.READ_CONTACTS)){
                    requestPermissions(Manifest.permission.READ_CONTACTS);
                }else {
                    getAllContacts();
                    Toast.makeText(MainActivity.this,"Contact data has been printed in the android monitor log..",Toast.LENGTH_LONG).show();

                }
            }

            private void getAllContacts() {

            }

            private void requestPermissions(String readContacts) {

            }

            private boolean hasPhoneContactsPermission(String readContacts) {
                return false;
            }
        });

    }*/

    private List<ContactDTO> getAllContacts() {
        List<ContactDTO> ret = new ArrayList<ContactDTO>();

        // Get all raw contacts id list.
        List<Integer> rawContactsIdList = getRawContactsIdList();

        int contactListSize = rawContactsIdList.size();

        ContentResolver contentResolver = getContentResolver();

        // Loop in the raw contacts list.
        for (int i = 0; i < contactListSize; i++) {
            // Get the raw contact id.
            Integer rawContactId = rawContactsIdList.get(i);

            Log.d(TAG_ANDROID_CONTACTS, "raw contact id : " + rawContactId.intValue());

            // Data content uri (access data table. )
            Uri dataContentUri = ContactsContract.Data.CONTENT_URI;

            // Build query columns name array.
            List<String> queryColumnList = new ArrayList<String>();

            // ContactsContract.Data.CONTACT_ID = "contact_id";
            queryColumnList.add(ContactsContract.Data.CONTACT_ID);

            // ContactsContract.Data.MIMETYPE = "mimetype";
            queryColumnList.add(ContactsContract.Data.MIMETYPE);

            queryColumnList.add(ContactsContract.Data.DATA1);
            queryColumnList.add(ContactsContract.Data.DATA2);
            queryColumnList.add(ContactsContract.Data.DATA3);
            queryColumnList.add(ContactsContract.Data.DATA4);
            queryColumnList.add(ContactsContract.Data.DATA5);
            queryColumnList.add(ContactsContract.Data.DATA6);
            queryColumnList.add(ContactsContract.Data.DATA7);
            queryColumnList.add(ContactsContract.Data.DATA8);
            queryColumnList.add(ContactsContract.Data.DATA9);
            queryColumnList.add(ContactsContract.Data.DATA10);
            queryColumnList.add(ContactsContract.Data.DATA11);
            queryColumnList.add(ContactsContract.Data.DATA12);
            queryColumnList.add(ContactsContract.Data.DATA13);
            queryColumnList.add(ContactsContract.Data.DATA14);
            queryColumnList.add(ContactsContract.Data.DATA15);

            // Translate column name list to array.
            String queryColumnArr[] = queryColumnList.toArray(new String[queryColumnList.size()]);

            // Build query condition string. Query rows by contact id.
            StringBuffer whereClauseBuf = new StringBuffer();
            whereClauseBuf.append(ContactsContract.Data.RAW_CONTACT_ID);
            whereClauseBuf.append("=");
            whereClauseBuf.append(rawContactId);

            // Query data table and return related contact data.
            Cursor cursor = contentResolver.query(dataContentUri, queryColumnArr, whereClauseBuf.toString(), null, null);

            /* If this cursor return database table row data.
               If do not check cursor.getCount() then it will throw error
               android.database.CursorIndexOutOfBoundsException: Index 0 requested, with a size of 0.
               */
            if (cursor != null && cursor.getCount() > 0) {
                StringBuffer lineBuf = new StringBuffer();
                cursor.moveToFirst();

                lineBuf.append("Raw Contact Id : ");
                lineBuf.append(rawContactId);

                long contactId = cursor.getLong(cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID));
                lineBuf.append(" , Contact Id : ");
                lineBuf.append(contactId);

                do {
                    // First get mimetype column value.
                    String mimeType = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.MIMETYPE));
                    lineBuf.append(" \r\n , MimeType : ");
                    lineBuf.append(mimeType);

                    List<String> dataValueList = getColumnValueByMimetype(cursor, mimeType);
                    int dataValueListSize = dataValueList.size();
                    for (int j = 0; j < dataValueListSize; j++) {
                        String dataValue = dataValueList.get(j);
                        lineBuf.append(" , ");
                        lineBuf.append(dataValue);
                    }

                } while (cursor.moveToNext());

                Log.d(TAG_ANDROID_CONTACTS, lineBuf.toString());
            }

        }

        return ret;
    }

    /*
     *  Get email type related string format value.
     * */
    private String getEmailTypeString(int dataType) {
        String ret = "";

        if (ContactsContract.CommonDataKinds.Email.TYPE_HOME == dataType) {
            ret = "Home";
        } else if (ContactsContract.CommonDataKinds.Email.TYPE_WORK == dataType) {
            ret = "Work";
        }
        return ret;
    }

    /*
     *  Get phone type related string format value.
     * */
    private String getPhoneTypeString(int dataType) {
        String ret = "";

        if (ContactsContract.CommonDataKinds.Phone.TYPE_HOME == dataType) {
            ret = "Home";
        } else if (ContactsContract.CommonDataKinds.Phone.TYPE_WORK == dataType) {
            ret = "Work";
        } else if (ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE == dataType) {
            ret = "Mobile";
        }
        return ret;
    }

    /*
     *  Return data column value by mimetype column value.
     *  Because for each mimetype there has not only one related value,
     *  such as Organization.CONTENT_ITEM_TYPE need return company, department, title, job description etc.
     *  So the return is a list string, each string for one column value.
     * */
    private List<String> getColumnValueByMimetype(Cursor cursor, String mimeType) {
        List<String> ret = new ArrayList<String>();

        switch (mimeType) {
            // Get email data.
            case ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE:
                // Email.ADDRESS == data1
                String emailAddress = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS));
                // Email.TYPE == data2
                int emailType = cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.TYPE));
                String emailTypeStr = getEmailTypeString(emailType);

                ret.add("Email Address : " + emailAddress);
                ret.add("Email Int Type : " + emailType);
                ret.add("Email String Type : " + emailTypeStr);
                break;

            // Get im data.
            case ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE:
                // Im.PROTOCOL == data5
                String imProtocol = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Im.PROTOCOL));
                // Im.DATA == data1
                String imId = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Im.DATA));

                ret.add("IM Protocol : " + imProtocol);
                ret.add("IM ID : " + imId);
                break;

            // Get nickname
            case ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE:
                // Nickname.NAME == data1
                String nickName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Nickname.NAME));
                ret.add("Nick name : " + nickName);
                break;

            // Get organization data.
            case ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE:
                // Organization.COMPANY == data1
                String company = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.COMPANY));
                // Organization.DEPARTMENT == data5
                String department = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.DEPARTMENT));
                // Organization.TITLE == data4
                String title = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.TITLE));
                // Organization.JOB_DESCRIPTION == data6
                String jobDescription = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.JOB_DESCRIPTION));
                // Organization.OFFICE_LOCATION == data9
                String officeLocation = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.OFFICE_LOCATION));

                ret.add("Company : " + company);
                ret.add("department : " + department);
                ret.add("Title : " + title);
                ret.add("Job Description : " + jobDescription);
                ret.add("Office Location : " + officeLocation);
                break;

            // Get phone number.
            case ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE:
                // Phone.NUMBER == data1
                String phoneNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                // Phone.TYPE == data2
                int phoneTypeInt = cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
                String phoneTypeStr = getPhoneTypeString(phoneTypeInt);

                ret.add("Phone Number : " + phoneNumber);
                ret.add("Phone Type Integer : " + phoneTypeInt);
                ret.add("Phone Type String : " + phoneTypeStr);
                break;

            // Get sip address.
            case ContactsContract.CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE:
                // SipAddress.SIP_ADDRESS == data1
                String address = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.SipAddress.SIP_ADDRESS));
                // SipAddress.TYPE == data2
                int addressTypeInt = cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.SipAddress.TYPE));
                String addressTypeStr = getEmailTypeString(addressTypeInt);

                ret.add("Address : " + address);
                ret.add("Address Type Integer : " + addressTypeInt);
                ret.add("Address Type String : " + addressTypeStr);
                break;

            // Get display name.
            case ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE:
                // StructuredName.DISPLAY_NAME == data1
                String displayName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME));
                // StructuredName.GIVEN_NAME == data2
                String givenName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME));
                // StructuredName.FAMILY_NAME == data3
                String familyName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME));

                ret.add("Display Name : " + displayName);
                ret.add("Given Name : " + givenName);
                ret.add("Family Name : " + familyName);
                break;

            // Get postal address.
            case ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE:
                // StructuredPostal.COUNTRY == data10
                String country = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY));
                // StructuredPostal.CITY == data7
                String city = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.CITY));
                // StructuredPostal.REGION == data8
                String region = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.REGION));
                // StructuredPostal.STREET == data4
                String street = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.STREET));
                // StructuredPostal.POSTCODE == data9
                String postcode = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE));
                // StructuredPostal.TYPE == data2
                int postType = cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.TYPE));
                String postTypeStr = getEmailTypeString(postType);

                ret.add("Country : " + country);
                ret.add("City : " + city);
                ret.add("Region : " + region);
                ret.add("Street : " + street);
                ret.add("Postcode : " + postcode);
                ret.add("Post Type Integer : " + postType);
                ret.add("Post Type String : " + postTypeStr);
                break;

            // Get identity.
            case ContactsContract.CommonDataKinds.Identity.CONTENT_ITEM_TYPE:
                // Identity.IDENTITY == data1
                String identity = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Identity.IDENTITY));
                // Identity.NAMESPACE == data2
                String namespace = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Identity.NAMESPACE));

                ret.add("Identity : " + identity);
                ret.add("Identity Namespace : " + namespace);
                break;

            // Get photo.
            case ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE:
                // Photo.PHOTO == data15
                // String photo = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Photo.PHOTO));
                // Photo.PHOTO_FILE_ID == data14
                String photoFileId = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Photo.PHOTO_FILE_ID));

                String photo = "";
                ret.add("Photo : " + photo);
                ret.add("Photo File Id: " + photoFileId);
                break;

            // Get group membership.
            case ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE:
                // GroupMembership.GROUP_ROW_ID == data1
                int groupId = cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID));
                ret.add("Group ID : " + groupId);
                break;

            // Get website.
            case ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE:
                // Website.URL == data1
                String websiteUrl = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Website.URL));
                // Website.TYPE == data2
                int websiteTypeInt = cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Website.TYPE));
                String websiteTypeStr = getEmailTypeString(websiteTypeInt);

                ret.add("Website Url : " + websiteUrl);
                ret.add("Website Type Integer : " + websiteTypeInt);
                ret.add("Website Type String : " + websiteTypeStr);
                break;

            // Get note.
            case ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE:
                // Note.NOTE == data1
                String note = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Note.NOTE));
                ret.add("Note : " + note);
                break;

        }

        return ret;
    }

    // Return all raw_contacts _id in a list.
    private List<Integer> getRawContactsIdList() {
        List<Integer> ret = new ArrayList<Integer>();

        ContentResolver contentResolver = getContentResolver();

        // Row contacts content uri( access raw_contacts table. ).
        Uri rawContactUri = ContactsContract.RawContacts.CONTENT_URI;
        // Return _id column in contacts raw_contacts table.
        String queryColumnArr[] = {ContactsContract.RawContacts._ID};
        // Query raw_contacts table and return raw_contacts table _id.
        Cursor cursor = contentResolver.query(rawContactUri, queryColumnArr, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            do {
                int idColumnIndex = cursor.getColumnIndex(ContactsContract.RawContacts._ID);
                int rawContactsId = cursor.getInt(idColumnIndex);
                ret.add(new Integer(rawContactsId));
            } while (cursor.moveToNext());
        }

        cursor.close();

        return ret;
    }


    // Check whether user has phone contacts manipulation permission or not.
    private boolean hasPhoneContactsPermission(String permission) {
        boolean ret = false;

        // If android sdk version is bigger than 23 the need to check run time permission.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            // return phone read contacts permission grant status.
            int hasPermission = ContextCompat.checkSelfPermission(getApplicationContext(), permission);
            // If permission is granted then return true.
            if (hasPermission == PackageManager.PERMISSION_GRANTED) {
                ret = true;
            }
        } else {
            ret = true;
        }
        return ret;
    }

    // Request a runtime permission to app user.
    private void requestPermission(String permission) {
        String requestPermissionArray[] = {permission};
        ActivityCompat.requestPermissions(this, requestPermissionArray, 1);
    }

    // After user select Allow or Deny button in request runtime permission dialog
    // , this method will be invoked.
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        int length = grantResults.length;
        if (length > 0) {
            int grantResult = grantResults[0];

            if (grantResult == PackageManager.PERMISSION_GRANTED) {

                Toast.makeText(getApplicationContext(), "You allowed permission, please click the button again.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(), "You denied permission.", Toast.LENGTH_LONG).show();
            }
        }
    }


    /*@NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {

        if (id == CONTACTS_LOADER_ID) {
            Loader<Cursor> contactsloader;
            return contactsloader();
        }
        return null;
    }

    private Loader<Cursor> contactsloader() {
        Uri contactUri = ContactsContract.Contacts.CONTENT_URI;
        String[] projection = new String[]{ContactsContract.Contacts.DISPLAY_NAME};
        String selection = null;
        String[] selectonArgs = {};
        String sortOrder = null;

        return new CursorLoader(getApplicationContext(), contactUri, projection, selection, selectonArgs, sortOrder);

    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        List<String> contacts = contactsFromCursor(cursor);

    }

    private List<String> contactsFromCursor(Cursor cursor) {
        List<String> contacts = new ArrayList<String>();
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            do {
                String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                contacts.add(name);
            } while (cursor.moveToNext());
        }
        return contacts;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.

    }*/


}