package in.teacher.sectionincharge;

import in.teacher.activity.R;
import in.teacher.dao.CCEStudentProfileDao;
import in.teacher.dao.StudentsDao;
import in.teacher.dao.TempDao;
import in.teacher.model.Profile;
import in.teacher.sqlite.CCEStudentProfile;
import in.teacher.sqlite.Students;
import in.teacher.sqlite.Temp;
import in.teacher.util.AppGlobal;
import in.teacher.util.CommonDialogUtils;
import in.teacher.util.ReplaceFragment;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Created by vinkrish.
 */
@SuppressLint("InflateParams")
public class InsertCCEStudentProfile extends Fragment {
    private Activity act;
    private SQLiteDatabase sqliteDatabase;
    private ProfileAdapter profileAdapter;
    private int sectionId, classId, schoolId, term;
    private ArrayList<Profile> profileList = new ArrayList<>();
    private List<Students> studentsArray;
    private EditText totalDays;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.enter_cce_student_profile, container, false);
        act = AppGlobal.getActivity();
        Context context = AppGlobal.getContext();
        sqliteDatabase = AppGlobal.getSqliteDatabase();

        Bundle b = getArguments();
        term = b.getInt("Term");

        Button insert = (Button) view.findViewById(R.id.insertUpdate);
        insert.setText("Insert");

        totalDays = (EditText) view.findViewById(R.id.today_days);
        Button submit = (Button) view.findViewById(R.id.submit);

        Temp t = TempDao.selectTemp(sqliteDatabase);
        sectionId = t.getSectionId();
        classId = t.getClassId();
        schoolId = t.getSchoolId();

        ListView lv = (ListView) view.findViewById(R.id.list);
        studentsArray = StudentsDao.selectStudents(sectionId, sqliteDatabase);
        for (Students stud : studentsArray) {
            profileList.add(new Profile(stud.getStudentId(), stud.getRollNoInClass() + "", stud.getName(), "", "", ""));
        }

        profileAdapter = new ProfileAdapter(context, R.layout.profile_adapter, profileList);
        lv.setAdapter(profileAdapter);

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!totalDays.getText().toString().equals("")) {
                    if (validateDate()) {
                        List<CCEStudentProfile> cspList = new ArrayList<>();
                        for (Profile p : profileList) {
                            CCEStudentProfile csp = new CCEStudentProfile();
                            csp.setSchoolId(schoolId + "");
                            csp.setClassId(classId + "");
                            csp.setSectionId(sectionId + "");
                            csp.setStudentId(p.getStudentId() + "");
                            csp.setStudentName(p.getName());
                            csp.setHeight(p.getHeight());
                            csp.setWeight(p.getWeight());
                            try {
                                csp.setDaysAttended1(Double.parseDouble(p.getDaysAttended()));
                            } catch (NumberFormatException e) {
                                csp.setDaysAttended1(0);
                            }
                            csp.setTotalDays1(Double.parseDouble(totalDays.getText().toString()));
                            csp.setTerm(term);
                            cspList.add(csp);
                        }
                        CCEStudentProfileDao.insertCCEStudentProfile(cspList, sqliteDatabase);
                        ReplaceFragment.replace(new SelectCCEStudentProfile(), getFragmentManager());
                    } else {
                        CommonDialogUtils.displayAlertWhiteDialog(act, "Days attended for one or more students is more than Total Days!");
                    }
                } else {
                    CommonDialogUtils.displayAlertWhiteDialog(act, "Please enter total number of days");
                }
            }
        });

        Button cceProfile = (Button) view.findViewById(R.id.cce_profile);
        cceProfile.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ReplaceFragment.replace(new SelectCCEStudentProfile(), getFragmentManager());
            }
        });

        return view;
    }

    private boolean validateDate() {
        boolean flag = true;
        for (Profile p : profileList) {
            try {
                if (Integer.parseInt(totalDays.getText().toString()) < Integer.parseInt(p.getDaysAttended())) {
                    flag = false;
                }
            } catch (NumberFormatException e) {
            }
        }
        return flag;
    }

    public class ProfileAdapter extends ArrayAdapter<Profile> {
        int resource;
        Context context;
        ArrayList<Profile> data = new ArrayList<>();
        LayoutInflater inflater = null;

        public ProfileAdapter(Context context, int resource, ArrayList<Profile> listArray) {
            super(context, resource, listArray);
            this.context = context;
            this.resource = resource;
            this.data = listArray;
            inflater = (LayoutInflater) context.getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            RecordHolder holder;

            if (row == null) {
                row = inflater.inflate(resource, parent, false);
                holder = new RecordHolder();
                holder.tv1 = (TextView) row.findViewById(R.id.roll);
                holder.tv2 = (TextView) row.findViewById(R.id.name);
                holder.tv3 = (TextView) row.findViewById(R.id.height);
                holder.tv4 = (TextView) row.findViewById(R.id.weight);
                holder.tv5 = (TextView) row.findViewById(R.id.days_attended);
                row.setTag(holder);
            } else {
                holder = (RecordHolder) row.getTag();
            }

            if (position % 2 == 0) {
                row.setBackgroundResource(R.drawable.list_selector1);
            } else {
                row.setBackgroundResource(R.drawable.list_selector2);
            }

            Profile listItem = data.get(position);
            holder.tv1.setText(listItem.getRoll());
            holder.tv2.setText(listItem.getName());
            holder.tv3.setText(listItem.getHeight());
            holder.tv4.setText(listItem.getWeight());
            holder.tv5.setText(listItem.getDaysAttended());

            holder.tv3.setOnClickListener(heightClickListener);
            holder.tv4.setOnClickListener(weightClickListener);
            holder.tv5.setOnClickListener(daysClickListener);

            return row;
        }

        class RecordHolder {
            TextView tv1;
            TextView tv2;
            TextView tv3;
            TextView tv4;
            TextView tv5;
        }
    }

    private OnClickListener heightClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            ListView mListView = (ListView) v.getParent().getParent();
            final int position = mListView.getPositionForView((View) v.getParent());
            Profile p = profileList.get(position);

            AlertDialog.Builder builder = new AlertDialog.Builder(act);
            View view = act.getLayoutInflater().inflate(R.layout.profile_dialog, null);
            TextView h = (TextView) view.findViewById(R.id.name_profile);
            h.setText(studentsArray.get(position).getName() + " - [Height]");
            final EditText edListChild = (EditText) view.findViewById(R.id.value);
            edListChild.setText(p.getHeight());
            edListChild.setSelection(edListChild.length());

            builder.setView(view);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String s = edListChild.getText().toString();
                    if (s.endsWith(".")) {
                        s = s.substring(0, s.length() - 1);
                    }
                    if (!s.equals("") && !s.equals(".")) {
                        Profile prof = new Profile(studentsArray.get(position).getStudentId(), studentsArray.get(position).getRollNoInClass() + "", studentsArray.get(position).getName(),
                                s.replaceAll("\n", " "), profileList.get(position).getWeight(), profileList.get(position).getDaysAttended());
                        profileList.set(position, prof);
                        profileAdapter.notifyDataSetChanged();
                    }
                }
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
        }
    };

    private OnClickListener weightClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            ListView mListView = (ListView) v.getParent().getParent();
            final int position = mListView.getPositionForView((View) v.getParent());
            Profile p = profileList.get(position);

            AlertDialog.Builder builder = new AlertDialog.Builder(act);
            View view = act.getLayoutInflater().inflate(R.layout.profile_dialog, null);
            TextView w = (TextView) view.findViewById(R.id.name_profile);
            w.setText(studentsArray.get(position).getName() + " - [Weight]");
            final EditText edListChild = (EditText) view.findViewById(R.id.value);
            edListChild.setText(p.getWeight());
            edListChild.setSelection(edListChild.length());

            builder.setView(view);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String s = edListChild.getText().toString();
                    if (s.endsWith(".")) {
                        s = s.substring(0, s.length() - 1);
                    }
                    if (!s.equals("") && !s.equals(".")) {
                        Profile prof = new Profile(studentsArray.get(position).getStudentId(), studentsArray.get(position).getRollNoInClass() + "", studentsArray.get(position).getName(),
                                profileList.get(position).getHeight(), s.replaceAll("\n", " "), profileList.get(position).getDaysAttended());
                        profileList.set(position, prof);
                        profileAdapter.notifyDataSetChanged();
                    }
                }
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
        }
    };

    private OnClickListener daysClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            ListView mListView = (ListView) v.getParent().getParent();
            final int position = mListView.getPositionForView((View) v.getParent());
            Profile p = profileList.get(position);

            AlertDialog.Builder builder = new AlertDialog.Builder(act);
            View view = act.getLayoutInflater().inflate(R.layout.profile_dialog, null);
            TextView dA = (TextView) view.findViewById(R.id.name_profile);
            dA.setText(studentsArray.get(position).getName() + " - [Days Attended]");
            final EditText edListChild = (EditText) view.findViewById(R.id.value);
            if (p.getDaysAttended().equals("0.0"))
                edListChild.setText("");
            else
                edListChild.setText(p.getDaysAttended());
            edListChild.setSelection(edListChild.length());

            builder.setView(view);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String s1 = edListChild.getText().toString();
                    String s2 = totalDays.getText().toString();
                    if (!s1.equals("") && !s1.equals(".")) {
                        if (!s2.equals("")) {
                            if (Double.parseDouble(s1) <= Integer.parseInt(s2)) {
                                Profile prof = new Profile(studentsArray.get(position).getStudentId(), studentsArray.get(position).getRollNoInClass() + "", studentsArray.get(position).getName(),
                                        profileList.get(position).getHeight(), profileList.get(position).getWeight(), edListChild.getText().toString().replaceAll("\n", " "));
                                profileList.set(position, prof);
                                profileAdapter.notifyDataSetChanged();
                            } else {
                                CommonDialogUtils.displayAlertWhiteDialog(act, "Entered day is greater than total days");
                            }
                        } else {
                            Profile prof = new Profile(studentsArray.get(position).getStudentId(), studentsArray.get(position).getRollNoInClass() + "", studentsArray.get(position).getName(),
                                    profileList.get(position).getHeight(), profileList.get(position).getWeight(), edListChild.getText().toString().replaceAll("\n", " "));
                            profileList.set(position, prof);
                            profileAdapter.notifyDataSetChanged();
                        }
                    }
                }
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
        }
    };

}