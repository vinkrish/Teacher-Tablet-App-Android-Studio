package in.teacher.fragment;

import in.teacher.activity.R;
import in.teacher.adapter.Capitalize;
import in.teacher.adapter.MarksAdapter;
import in.teacher.adapter.StudentsSort;
import in.teacher.dao.ActivitiDao;
import in.teacher.dao.ActivityMarkDao;
import in.teacher.dao.ClasDao;
import in.teacher.dao.ExamsDao;
import in.teacher.dao.ExmAvgDao;
import in.teacher.dao.SectionDao;
import in.teacher.dao.StudentsDao;
import in.teacher.dao.SubActivityDao;
import in.teacher.dao.SubActivityMarkDao;
import in.teacher.dao.SubjectExamsDao;
import in.teacher.dao.TempDao;
import in.teacher.sqlite.Activiti;
import in.teacher.sqlite.Students;
import in.teacher.sqlite.SubActivity;
import in.teacher.sqlite.SubActivityMark;
import in.teacher.sqlite.Temp;
import in.teacher.util.AppGlobal;
import in.teacher.util.PKGenerator;
import in.teacher.util.ReplaceFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;

public class InsertSubActivityMark extends Fragment {
    private Activity activity;
    private Context context;
    private SQLiteDatabase sqliteDatabase;
    private int sectionId;
    private String activityName, subActivityName;
    private List<Students> studentsArray = new ArrayList<>();
    private List<Boolean> studentIndicate = new ArrayList<>();
    private ArrayList<Students> studentsArrayList = new ArrayList<>();
    private List<String> studentScore = new ArrayList<>();
    private ListView lv;
    private MarksAdapter marksAdapter;
    private int index = 0, indexBound, top, lastVisible, firstVisible, totalVisible;
    private int schoolId, examId, subjectId, subId, classId, activityId, subActivityId, calculation;
    private float maxMark;
    private StringBuffer sf = new StringBuffer();
    private TextView clasSecSub;
    private Bitmap empty, entered;
    private SharedPreferences sharedPref;
    private Button previous, next, submit, clear;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.mark_score, container, false);

        activity = AppGlobal.getActivity();
        context = AppGlobal.getContext();
        sqliteDatabase = AppGlobal.getSqliteDatabase();
        sharedPref = context.getSharedPreferences("has_partition", Context.MODE_PRIVATE);

        lv = (ListView) view.findViewById(R.id.list);
        marksAdapter = new MarksAdapter(context, studentsArrayList);
        lv.setAdapter(marksAdapter);

        initView(view);

        lv.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                if (studentScore.get(index) != null
                        && !studentScore.get(index).equals("")
                        && studentScore.get(index).equals("."))
                    studentScore.set(index, "");
                if (studentScore.get(index) != null
                        && !studentScore.get(index).equals("")
                        && Double.parseDouble(studentScore.get(index)) > maxMark) {
                    studentScore.set(index, "");
                    Toast.makeText(context, "marks entered is greater than max mark", Toast.LENGTH_SHORT).show();
                }
                index = pos;
                View v = lv.getChildAt(0);
                top = (v == null) ? 0 : v.getTop();
                for (int idx = 0; idx < studentsArray.size(); idx++)
                    studentIndicate.set(idx, false);

                Boolean b = studentIndicate.get(index);
                if (!b) studentIndicate.set(index, true);

                repopulateListArray();
            }
        });

        new CalledBackLoad().execute();

        lv.setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {}
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {
                firstVisible = lv.getFirstVisiblePosition();
                lastVisible = lv.getLastVisiblePosition();
                totalVisible = lastVisible - firstVisible;
            }
        });

        initButton(view);

        view.findViewById(R.id.enter_grade).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ReplaceFragment.replace(new InsertSubActivityGrade(), getFragmentManager());
            }
        });

        return view;
    }

    private void initView(View view) {
        clasSecSub = (TextView) view.findViewById(R.id.clasSecSub);
        empty = BitmapFactory.decodeResource(this.getResources(), R.drawable.deindicator);
        entered = BitmapFactory.decodeResource(this.getResources(), R.drawable.indicator);

        previous = (Button) view.findViewById(R.id.previous);
        next = (Button) view.findViewById(R.id.next);
        submit = (Button) view.findViewById(R.id.submit);
        clear = (Button) view.findViewById(R.id.clear);

        Temp t = TempDao.selectTemp(sqliteDatabase);
        schoolId = t.getSchoolId();
        classId = t.getCurrentClass();
        sectionId = t.getCurrentSection();
        subjectId = t.getCurrentSubject();
        subId = t.getSubjectId();
        examId = t.getExamId();
        activityId = t.getActivityId();
        subActivityId = t.getSubActivityId();

        activityName = ActivitiDao.selectActivityName(activityId, sqliteDatabase);

        SubActivity tempSubAct = SubActivityDao.getSubActivity(subActivityId, sqliteDatabase);
        calculation = tempSubAct.getCalculation();
        maxMark = tempSubAct.getMaximumMark();
        subActivityName = tempSubAct.getSubActivityName();

        TextView maxMarkView = (TextView) view.findViewById(R.id.maxmark);
        maxMarkView.setText(maxMark + "");
    }

    private void initButton(final View view) {
        int[] buttonIds = {R.id.one, R.id.two, R.id.three, R.id.four, R.id.five, R.id.six, R.id.seven,
                R.id.eight, R.id.nine, R.id.zero, R.id.decimal, R.id.minus};
        for (int i = 0; i < 12; i++) {
            Button b = (Button) view.findViewById(buttonIds[i]);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (v.getId() == R.id.one) updateScoreField("1");
                    if (v.getId() == R.id.two) updateScoreField("2");
                    if (v.getId() == R.id.three) updateScoreField("3");
                    if (v.getId() == R.id.four) updateScoreField("4");
                    if (v.getId() == R.id.five) updateScoreField("5");
                    if (v.getId() == R.id.six) updateScoreField("6");
                    if (v.getId() == R.id.seven) updateScoreField("7");
                    if (v.getId() == R.id.eight) updateScoreField("8");
                    if (v.getId() == R.id.nine) updateScoreField("9");
                    if (v.getId() == R.id.zero) updateScoreField("0");
                    if (v.getId() == R.id.decimal) updateScoreField(".");
                    if (v.getId() == R.id.minus) {
                        studentScore.set(index, "-1");
                        repopulateListArray();
                    }
                }
            });
        }

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (studentScore.get(index) != null
                        && !studentScore.get(index).equals("")
                        && !studentScore.get(index).equals(".")
                        && Double.parseDouble(studentScore.get(index)) > maxMark) {
                    String s = "";
                    studentScore.set(index, s);
                    Toast.makeText(context, "Marks Entered is Greater than Max Mark", Toast.LENGTH_SHORT).show();
                    repopulateListArray();
                } else {
                    Toast.makeText(context, "marks entered has been saved", Toast.LENGTH_LONG).show();
                    new CalledSubmit().execute();
                }
            }
        });

        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                studentScore.set(index, "");
                repopulateListArray();
            }
        });

        previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (studentScore.get(index) != null
                        && !studentScore.get(index).equals("")
                        && studentScore.get(index).equals(".")) {
                    studentScore.set(index, "");
                }
                if (studentScore.get(index) != null
                        && !studentScore.get(index).equals("")
                        && Double.parseDouble(studentScore.get(index)) > maxMark) {
                    String s = "";
                    studentScore.set(index, s);
                    Toast.makeText(context, "marks entered is greater than max mark", Toast.LENGTH_SHORT).show();
                } else {
                    if (index != 0) index--;
                    for (int idx = 0; idx < studentsArray.size(); idx++)
                        studentIndicate.set(idx, false);

                    Boolean b = studentIndicate.get(index);
                    if (!b) studentIndicate.set(index, true);

                }
                repopulateListArray();
            }
        });
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (studentScore.get(index) != null
                        && !studentScore.get(index).equals("")
                        && studentScore.get(index).equals(".")) {
                    studentScore.set(index, "");
                }
                if (studentScore.get(index) != null
                        && !studentScore.get(index).equals("")
                        && Double.parseDouble(studentScore.get(index)) > maxMark) {
                    String s = "";
                    studentScore.set(index, s);
                    Toast.makeText(context, "marks entered is greater than max mark", Toast.LENGTH_SHORT).show();
                } else {
                    if (index < indexBound - 1) index++;
                    for (int idx = 0; idx < studentsArray.size(); idx++)
                        studentIndicate.set(idx, false);

                    Boolean b = studentIndicate.get(index);
                    if (!b) studentIndicate.set(index, true);
                }
                repopulateListArray();
            }
        });
    }

    class CalledSubmit extends AsyncTask<Void, Void, Void> {
        ProgressDialog pDialog = new ProgressDialog(activity);

        protected void onPreExecute() {
            super.onPreExecute();
            pDialog.setMessage("Submitting marks...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            pushSubmit();
            return null;
        }

        protected void onPostExecute(Void v) {
            super.onPostExecute(v);
            pDialog.dismiss();
            ReplaceFragment.replace(new SubActivityExam(), getFragmentManager());
        }

    }

    private void pushSubmit() {
        int i = 0;
        for (String ss : studentScore) {
            if (ss == null || ss.equals(".") || ss.equals("")) studentScore.set(i, "0");
            i++;
        }
        int j = 0;
        List<SubActivityMark> mList = new ArrayList<>();
        for (Students st : studentsArray) {
            SubActivityMark m = new SubActivityMark();
            m.setSchoolId(schoolId);
            m.setExamId(examId);
            m.setActivityId(activityId);
            m.setSubActivityId(subActivityId);
            m.setSubjectId(subjectId);
            m.setStudentId(st.getStudentId());
            m.setMark(studentScore.get(j));
            mList.add(m);
            j++;
        }
        SubActivityMarkDao.insertSubActivityMark(mList, sqliteDatabase);
        int entry = ExmAvgDao.checkExmEntry(sectionId, subjectId, examId, sqliteDatabase);
        if (entry == 0)
            ExmAvgDao.insertIntoExmAvg(classId, sectionId, subjectId, examId, schoolId, sqliteDatabase);
        SubActivityDao.updateSubActivityAvg(subActivityId, schoolId, sqliteDatabase);
        ActivitiDao.updateSubactActAvg(activityId, schoolId, sqliteDatabase);
        ExmAvgDao.updateActExmAvg(sectionId, subjectId, examId, schoolId, sqliteDatabase);
        SubActivityDao.checkSubActMarkEmpty(subActivityId, schoolId, sqliteDatabase);
        ActivitiDao.checkActSubActMarkEmpty(activityId, schoolId, sqliteDatabase);
        ExmAvgDao.checkExmSubActMarkEmpty(examId, sectionId, subjectId, schoolId, sqliteDatabase);
        activityWeightage();
    }

    private void activityWeightage() {
        List<SubActivity> subActList = SubActivityDao.selectSubActivity(activityId, sqliteDatabase);
        List<Integer> subActIdList = new ArrayList<>();
        List<Integer> weightageList = new ArrayList<>();
        List<Float> subActMaxMarkList = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        for (SubActivity subAct : subActList) {
            sb.append(subAct.getSubActivityId() + ",");
            subActIdList.add(subAct.getSubActivityId());
            weightageList.add(subAct.getWeightage());
            subActMaxMarkList.add(subAct.getMaximumMark());
        }
        boolean exist = SubActivityMarkDao.isAllSubActMarkExist(subActIdList, sqliteDatabase);
        if (exist) {
            float activityMaxMark = ActivitiDao.getActivityMaxMark(activityId, sqliteDatabase);
            List<Float> weightMarkList = new ArrayList<>();
            if (calculation == 0) {
                for (int i = 0; i < subActList.size(); i++) {
                    weightMarkList.add((float) (weightageList.get(i) / 100.0) * activityMaxMark);
                }
                List<Float> markList = new ArrayList<>();
                for (Students st : studentsArray) {
                    markList.clear();
                    for (int j = 0; j < subActList.size(); j++) {
                        float mark = 0;
                        Cursor c = sqliteDatabase.rawQuery("select Mark from subactivitymark where StudentId=" + st.getStudentId() + " and SubActivityId=" + subActIdList.get(j), null);
                        c.moveToFirst();
                        while (!c.isAfterLast()) {
                            mark = c.getFloat(c.getColumnIndex("Mark"));
                            c.moveToNext();
                        }
                        c.close();
                        markList.add((float) (mark / subActMaxMarkList.get(j)) * weightMarkList.get(j));
                    }
                    float finalMark = 0;
                    for (Float flo : markList)
                        finalMark += flo;

                    String sql = "insert into activitymark(SchoolId, ExamId, SubjectId, StudentId, ActivityId, Mark) " +
                            "values(" + schoolId + "," + examId + "," + subjectId + "," + st.getStudentId() + "," + activityId + ",'" + finalMark + "')";
                    try {
                        sqliteDatabase.execSQL(sql);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    ContentValues cv = new ContentValues();
                    cv.put("Query", sql);
                    sqliteDatabase.insert("uploadsql", null, cv);
                }
            } else if (calculation == -1) {
                Float subActMaxMark = 0f;
                for (Float f : subActMaxMarkList)
                    subActMaxMark += f;

                for (Students st : studentsArray) {
                    String sql = "insert into activitymark(SchoolId, ExamId, SubjectId, StudentId, ActivityId, Mark) " +
                            "values(" + schoolId + "," + examId + "," + subjectId + "," + st.getStudentId() + "," + activityId + "," +
                            "((select SUM(Mark) from subactivitymark where SubActivityId in (" + sb.substring(0, sb.length() - 1) + ") and " +
                            "StudentId=" + st.getStudentId() + ")/" + subActMaxMark + ")*" + activityMaxMark + ")";
                    try {
                        sqliteDatabase.execSQL(sql);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    ContentValues cv = new ContentValues();
                    cv.put("Query", sql);
                    sqliteDatabase.insert("uploadsql", null, cv);
                }
            }
        }
        examWeightage();
    }

    private void examWeightage() {
        List<Activiti> actList = ActivitiDao.selectActiviti(examId, subjectId, sectionId, sqliteDatabase);
        List<Integer> actIdList = new ArrayList<>();
        List<Integer> weightageList = new ArrayList<>();
        List<Float> actMaxMarkList = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        for (Activiti Act : actList) {
            sb.append(+Act.getActivityId() + ",");
            actIdList.add(Act.getActivityId());
            weightageList.add(Act.getWeightage());
            actMaxMarkList.add(Act.getMaximumMark());
        }
        boolean exist = ActivityMarkDao.isAllActMarkExist(actIdList, sqliteDatabase);
        if (exist) {
            float exmMaxMark = SubjectExamsDao.getExmMaxMark(classId, examId, subjectId, sqliteDatabase);
            List<Float> weightMarkList = new ArrayList<>();
            if (calculation == 0) {
                for (int i = 0; i < actList.size(); i++) {
                    weightMarkList.add((float) (weightageList.get(i) / 100.0) * exmMaxMark);
                }
                List<Float> markList = new ArrayList<>();
                for (Students st : studentsArray) {
                    markList.clear();
                    for (int j = 0; j < actList.size(); j++) {
                        float mark = 0;
                        Cursor c = sqliteDatabase.rawQuery("select Mark from activitymark where StudentId=" + st.getStudentId() + " and ActivityId=" + actIdList.get(j), null);
                        c.moveToFirst();
                        while (!c.isAfterLast()) {
                            mark = c.getFloat(c.getColumnIndex("Mark"));
                            c.moveToNext();
                        }
                        c.close();
                        markList.add((float) (mark / actMaxMarkList.get(j)) * weightMarkList.get(j));
                    }
                    float finalMark = 0;
                    for (Float flo : markList)
                        finalMark += flo;

                    String sql = "insert into marks(SchoolId, ExamId, SubjectId, StudentId, Mark) values(" +
                            schoolId + "," + examId + "," + subjectId + "," + st.getStudentId() + ",'" + finalMark + "')";
                    try {
                        sqliteDatabase.execSQL(sql);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    ContentValues cv = new ContentValues();
                    cv.put("Query", sql);
                    sqliteDatabase.insert("uploadsql", null, cv);
                }
            } else if (calculation == -1) {
                Float actMaxMark = 0f;
                for (Float f : actMaxMarkList)
                    actMaxMark += f;
                for (Students st : studentsArray) {
                    //		String sql = "insert into marks(SchoolId, ExamId, SubjectId, StudentId, Mark) values("+
                    //				schoolId+","+examId+","+subjectId+","+st.getStudentId()+"," +
                    //				"(select SUM(Mark) from activitymark where ActivityId in ("+sb.substring(0, sb.length()-1)+") and StudentId="+st.getStudentId()+"))";
                    String sql = "insert into marks(SchoolId, ExamId, SubjectId, StudentId, Mark) values(" +
                            schoolId + "," + examId + "," + subjectId + "," + st.getStudentId() + "," +
                            "((select SUM(Mark) from activitymark where ActivityId in (" + sb.substring(0, sb.length() - 1) + ") and " +
                            "StudentId=" + st.getStudentId() + ")/" + actMaxMark + ")*" + exmMaxMark + ")";
                    try {
                        sqliteDatabase.execSQL(sql);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    ContentValues cv = new ContentValues();
                    cv.put("Query", sql);
                    sqliteDatabase.insert("uploadsql", null, cv);
                }
            }
        }
    }

    private void updateScoreField(String upScore) {
        try {
            if (studentScore.get(index) != null
                    && !studentScore.get(index).equals("")
                    && !studentScore.get(index).equals("-1")) {
                studentScore.set(index, studentScore.get(index) + upScore);
            } else {
                studentScore.set(index, upScore);
            }
        } catch (NumberFormatException e) {
            studentScore.set(index, upScore);
        }
        repopulateListArray();
    }

    private void populateListArray() {
        indexBound = studentsArray.size();
        int idx = 0;
        for (Students s : studentsArray) {
            if (idx == 0) {
                studentIndicate.set(idx, true);
                studentsArrayList.add(new Students(s.getRollNoInClass(), Capitalize.capitalThis(s.getName()), s.getScore(), entered));
            } else {
                studentsArrayList.add(new Students(s.getRollNoInClass(), Capitalize.capitalThis(s.getName()), s.getScore(), empty));
            }
            studentScore.add(s.getScore());
            idx++;
        }
        marksAdapter.notifyDataSetChanged();
        lv.setSelection(index);
        //	lv.performItemClick(lv.getAdapter().getView(index, null, null), index, lv.getItemIdAtPosition(index));
    }

    private void repopulateListArray() {
        studentsArrayList.clear();
        int idx = 0;
        for (Students s : studentsArray) {
            if (studentIndicate.get(idx)) {
                studentsArrayList.add(new Students(s.getRollNoInClass(), Capitalize.capitalThis(s.getName()), studentScore.get(idx), entered));
            } else {
                studentsArrayList.add(new Students(s.getRollNoInClass(), Capitalize.capitalThis(s.getName()), studentScore.get(idx), empty));
            }
            idx++;
        }
        marksAdapter.notifyDataSetChanged();
        if (index == lastVisible) lv.setSelectionFromTop(index - 1, top);
        else if (index < firstVisible) lv.setSelectionFromTop(index, firstVisible - totalVisible);
        else lv.setSelection(firstVisible);
    }

    class CalledBackLoad extends AsyncTask<String, String, String> {
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {
            String subjectName = SubjectExamsDao.selectSubjectName(subjectId, sqliteDatabase);
            String className = ClasDao.getClassName(classId, sqliteDatabase);
            String sectionName = SectionDao.getSectionName(sectionId, sqliteDatabase);

            String examName = ExamsDao.selectExamName(examId, sqliteDatabase);

            sf.append(className).append("-").append(sectionName).append("   " + subjectName).append("   " + examName)
                    .append("   " + activityName).append("   " + subActivityName);

            int partition = sharedPref.getInt("partition", 0);
            if (partition == 1)
                studentsArray = StudentsDao.selectStudents2(sectionId, subId, sqliteDatabase);
            else
                studentsArray = StudentsDao.selectStudents2(sectionId, subjectId, sqliteDatabase);

            Collections.sort(studentsArray, new StudentsSort());
            for (int idx = 0; idx < studentsArray.size(); idx++)
                studentIndicate.add(false);

            return null;
        }

        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            clasSecSub.setText(PKGenerator.trim(0, 52, sf.toString()));
            populateListArray();
            if (studentsArray.size() == 0) {
                Toast.makeText(context, "No students!", Toast.LENGTH_SHORT).show();
                ReplaceFragment.replace(new SubActivityExam(), getFragmentManager());
            }
        }
    }
}
