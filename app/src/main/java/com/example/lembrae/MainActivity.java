


package com.example.lembrae;

import android.Manifest;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // COMPONENTES PRINCIPAIS
    private EditText edtTask;
    private Button btnAdd;
    private Button btnChooseTime;
    private Button btnChooseDate;
    private TextView txtSelectedTime;
    private TextView txtSelectedDate;
    private LinearLayout layoutTasks;

    // COMPONENTES DO CALENDÁRIO
    private GridLayout gridCalendar;
    private TextView txtCalendarMonth;
    private TextView txtCalendarSelected;
    private LinearLayout layoutCalendarTasks;
    private Button btnPreviousMonth;
    private Button btnNextMonth;

    // CONTROLE DO CALENDÁRIO
    private Calendar calendarDisplayed = Calendar.getInstance();
    private Calendar calendarSelected = Calendar.getInstance();

    // ARMAZENAMENTO DAS ATIVIDADES
    private JSONArray tasks;

    // HORÁRIO ESCOLHIDO
    private int selectedHour = -1;
    private int selectedMinute = -1;

    // DATA ESCOLHIDA PARA NOVA ATIVIDADE
    private int selectedYear;
    private int selectedMonth;
    private int selectedDay;

    // CONTROLE DA TROCA DE HORÁRIOS
    private int selectedSwapPosition = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // COMPONENTES PRINCIPAIS
        edtTask = findViewById(R.id.edtTask);
        btnAdd = findViewById(R.id.btnAdd);
        btnChooseTime = findViewById(R.id.btnChooseTime);
        btnChooseDate = findViewById(R.id.btnChooseDate);
        txtSelectedTime = findViewById(R.id.txtSelectedTime);
        txtSelectedDate = findViewById(R.id.txtSelectedDate);
        layoutTasks = findViewById(R.id.layoutTasks);

        // COMPONENTES DO CALENDÁRIO
        gridCalendar = findViewById(R.id.gridCalendar);
        txtCalendarMonth = findViewById(R.id.txtCalendarMonth);
        txtCalendarSelected = findViewById(R.id.txtCalendarSelected);
        layoutCalendarTasks = findViewById(R.id.layoutCalendarTasks);
        btnPreviousMonth = findViewById(R.id.btnPreviousMonth);
        btnNextMonth = findViewById(R.id.btnNextMonth);

        gridCalendar.setColumnCount(7);

        // DATA INICIAL
        Calendar today = Calendar.getInstance();

        selectedYear = today.get(Calendar.YEAR);
        selectedMonth = today.get(Calendar.MONTH);
        selectedDay = today.get(Calendar.DAY_OF_MONTH);

        calendarSelected = (Calendar) today.clone();
        calendarDisplayed = (Calendar) today.clone();

        // PERMISSÃO DE NOTIFICAÇÃO
        requestNotificationPermission();

        // CARREGA AS ATIVIDADES
        loadTasks();

        // BOTÕES DE DATA E HORÁRIO
        btnChooseTime.setOnClickListener(v -> chooseTime());
        btnChooseDate.setOnClickListener(v -> chooseDate());
        btnAdd.setOnClickListener(v -> addTask());

        // NAVEGAÇÃO DO CALENDÁRIO
        btnPreviousMonth.setOnClickListener(v -> {
            calendarDisplayed.add(Calendar.MONTH, -1);
            showCalendar();
        });

        btnNextMonth.setOnClickListener(v -> {
            calendarDisplayed.add(Calendar.MONTH, 1);
            showCalendar();
        });

        // MOSTRA AS ATIVIDADES E O CALENDÁRIO
        showTasks();
        showCalendar();

        // PROGRAMA OS LEMBRETES SALVOS
        for (int i = 0; i < tasks.length(); i++) {
            try {
                scheduleReminder(tasks.getJSONObject(i));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // SOLICITAR PERMISSÃO PARA NOTIFICAÇÕES
    private void requestNotificationPermission() {

        if (Build.VERSION.SDK_INT >= 33) {

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission.POST_NOTIFICATIONS
                        },
                        100
                );
            }
        }
    }

    // ESCOLHER HORÁRIO
    private void chooseTime() {

        Calendar calendar = Calendar.getInstance();

        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog dialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minuteOfDay) -> {

                    selectedHour = hourOfDay;
                    selectedMinute = minuteOfDay;

                    String time = String.format(
                            Locale.getDefault(),
                            "%02d:%02d",
                            selectedHour,
                            selectedMinute
                    );

                    txtSelectedTime.setText(
                            "Horário escolhido: " + time
                    );
                },
                hour,
                minute,
                true
        );

        dialog.show();
    }

    // ESCOLHER DATA
    private void chooseDate() {

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {

                    selectedYear = year;
                    selectedMonth = month;
                    selectedDay = dayOfMonth;

                    String date = String.format(
                            Locale.getDefault(),
                            "%02d/%02d/%04d",
                            selectedDay,
                            selectedMonth + 1,
                            selectedYear
                    );

                    txtSelectedDate.setText(
                            "Data escolhida: " + date
                    );
                },
                selectedYear,
                selectedMonth,
                selectedDay
        );

        dialog.getDatePicker().setMinDate(
                Calendar.getInstance().getTimeInMillis()
        );

        dialog.show();
    }

    // ADICIONAR ATIVIDADE
    private void addTask() {

        String taskName = edtTask.getText().toString().trim();

        if (taskName.isEmpty()) {
            Toast.makeText(
                    this,
                    "Digite uma atividade!",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (selectedHour == -1) {
            Toast.makeText(
                    this,
                    "Escolha um horário para a atividade!",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        Calendar selectedDateTime = Calendar.getInstance();

        selectedDateTime.set(
                selectedYear,
                selectedMonth,
                selectedDay,
                selectedHour,
                selectedMinute,
                0
        );

        selectedDateTime.set(Calendar.MILLISECOND, 0);

        if (selectedDateTime.getTimeInMillis()
                <= System.currentTimeMillis()) {

            Toast.makeText(
                    this,
                    "Escolha uma data e horário futuros!",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        try {

            JSONObject task = new JSONObject();

            String taskId = String.valueOf(
                    System.currentTimeMillis()
            );

            task.put("id", taskId);
            task.put("name", taskName);
            task.put("completed", false);
            task.put("hour", selectedHour);
            task.put("minute", selectedMinute);
            task.put("year", selectedYear);
            task.put("month", selectedMonth);
            task.put("day", selectedDay);

            tasks.put(task);

            // ATUALIZA O CALENDÁRIO PARA A DATA DA ATIVIDADE
            calendarSelected.set(
                    selectedYear,
                    selectedMonth,
                    selectedDay
            );

            calendarDisplayed.set(
                    selectedYear,
                    selectedMonth,
                    1
            );

            saveTasks();
            scheduleReminder(task);

            // LIMPA OS CAMPOS
            edtTask.setText("");

            selectedHour = -1;
            selectedMinute = -1;

            txtSelectedTime.setText(
                    "Nenhum horário selecionado"
            );

            Calendar today = Calendar.getInstance();

            selectedYear = today.get(Calendar.YEAR);
            selectedMonth = today.get(Calendar.MONTH);
            selectedDay = today.get(Calendar.DAY_OF_MONTH);

            txtSelectedDate.setText(
                    "Nenhuma data selecionada"
            );

            showTasks();
            showCalendar();

            Toast.makeText(
                    this,
                    "Atividade adicionada com sucesso!",
                    Toast.LENGTH_SHORT
            ).show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // MOSTRAR ATIVIDADES NA LISTA PRINCIPAL
    private void showTasks() {

        layoutTasks.removeAllViews();

        for (int i = 0; i < tasks.length(); i++) {

            final int position = i;

            try {

                JSONObject task = tasks.getJSONObject(i);

                String name = task.getString("name");
                boolean completed = task.getBoolean("completed");

                int hour = task.getInt("hour");
                int minute = task.getInt("minute");

                int year = task.getInt("year");
                int month = task.getInt("month");
                int day = task.getInt("day");

                String time = String.format(
                        Locale.getDefault(),
                        "%02d:%02d",
                        hour,
                        minute
                );

                String date = String.format(
                        Locale.getDefault(),
                        "%02d/%02d/%04d",
                        day,
                        month + 1,
                        year
                );

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.VERTICAL);
                row.setPadding(8, 12, 8, 12);

                CheckBox checkBox = new CheckBox(this);

                checkBox.setText(
                        name + "\n" + date + " às " + time
                );

                checkBox.setTextSize(16);
                checkBox.setTextColor(Color.DKGRAY);
                checkBox.setChecked(completed);

                row.addView(checkBox);

                LinearLayout buttons = new LinearLayout(this);
                buttons.setOrientation(LinearLayout.HORIZONTAL);

                Button swapButton = new Button(this);
                swapButton.setText("Trocar horário");

                Button deleteButton = new Button(this);
                deleteButton.setText("Excluir");

                buttons.addView(swapButton);
                buttons.addView(deleteButton);

                row.addView(buttons);

                // CONCLUIR OU REABRIR ATIVIDADE
                checkBox.setOnCheckedChangeListener(
                        (buttonView, isChecked) -> {

                            try {

                                JSONObject currentTask =
                                        tasks.getJSONObject(position);

                                currentTask.put(
                                        "completed",
                                        isChecked
                                );

                                if (isChecked) {
                                    cancelReminder(currentTask);
                                } else {
                                    scheduleReminder(currentTask);
                                }

                                saveTasks();
                                showCalendarTasks();

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                );

                // TROCAR HORÁRIO
                swapButton.setOnClickListener(v -> {

                    if (selectedSwapPosition == -1) {

                        selectedSwapPosition = position;

                        Toast.makeText(
                                this,
                                "Agora escolha outra atividade!",
                                Toast.LENGTH_SHORT
                        ).show();

                    } else if (selectedSwapPosition == position) {

                        selectedSwapPosition = -1;

                        Toast.makeText(
                                this,
                                "Troca cancelada.",
                                Toast.LENGTH_SHORT
                        ).show();

                    } else {

                        swapTimes(
                                selectedSwapPosition,
                                position
                        );

                        selectedSwapPosition = -1;
                    }
                });

                // EXCLUIR ATIVIDADE
                deleteButton.setOnClickListener(v -> {

                    try {

                        JSONObject deletedTask =
                                tasks.getJSONObject(position);

                        cancelReminder(deletedTask);

                        JSONArray updatedTasks = new JSONArray();

                        for (int j = 0; j < tasks.length(); j++) {

                            if (j != position) {
                                updatedTasks.put(
                                        tasks.getJSONObject(j)
                                );
                            }
                        }

                        tasks = updatedTasks;

                        saveTasks();

                        selectedSwapPosition = -1;

                        showTasks();
                        showCalendar();

                        Toast.makeText(
                                this,
                                "Atividade excluída!",
                                Toast.LENGTH_SHORT
                        ).show();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                layoutTasks.addView(row);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // CALENDÁRIO: MOSTRAR MÊS E DIAS
    private void showCalendar() {

        gridCalendar.removeAllViews();
        gridCalendar.setColumnCount(7);

        String monthName = calendarDisplayed.getDisplayName(
                Calendar.MONTH,
                Calendar.LONG,
                new Locale("pt", "BR")
        );

        int year = calendarDisplayed.get(Calendar.YEAR);

        String title = monthName.substring(0, 1).toUpperCase(
                new Locale("pt", "BR")
        ) + monthName.substring(1) + " " + year;

        txtCalendarMonth.setText(title);

        Calendar firstDay = (Calendar) calendarDisplayed.clone();
        firstDay.set(Calendar.DAY_OF_MONTH, 1);

        int daysInMonth = firstDay.getActualMaximum(
                Calendar.DAY_OF_MONTH
        );

        // DOMINGO = 0, SEGUNDA = 1...
        int offset = firstDay.get(Calendar.DAY_OF_WEEK)
                - Calendar.SUNDAY;

        // ESPAÇOS ANTES DO PRIMEIRO DIA
        for (int i = 0; i < offset; i++) {
            addCalendarSpace();
        }

        // CRIA OS DIAS DO MÊS
        for (int day = 1; day <= daysInMonth; day++) {

            final int selectedDayOfMonth = day;

            LinearLayout dayCell = new LinearLayout(this);
            dayCell.setOrientation(LinearLayout.VERTICAL);
            dayCell.setGravity(Gravity.CENTER);
            dayCell.setPadding(2, 5, 2, 5);

            TextView dayText = new TextView(this);
            dayText.setText(String.valueOf(day));
            dayText.setTextSize(14);
            dayText.setGravity(Gravity.CENTER);

            boolean isSelected =
                    calendarSelected.get(Calendar.YEAR) == year
                            && calendarSelected.get(Calendar.MONTH)
                            == calendarDisplayed.get(Calendar.MONTH)
                            && calendarSelected.get(Calendar.DAY_OF_MONTH) == day;

            if (isSelected) {

                dayText.setTextColor(Color.WHITE);
                dayText.setBackground(createCircle("#7955B5"));

            } else {

                dayText.setTextColor(Color.DKGRAY);
            }

            dayText.setWidth(dp(34));
            dayText.setHeight(dp(34));

            dayCell.addView(dayText);

            // PONTO PARA INDICAR ATIVIDADE
            TextView dot = new TextView(this);
            dot.setText("●");
            dot.setTextSize(7);
            dot.setGravity(Gravity.CENTER);

            if (hasTasksOnDate(year,
                    calendarDisplayed.get(Calendar.MONTH), day)) {

                dot.setTextColor(Color.rgb(121, 85, 181));

            } else {

                dot.setText("");
            }

            dayCell.addView(dot);

            GridLayout.LayoutParams params =
                    new GridLayout.LayoutParams();

            params.width = 0;
            params.height = dp(54);
            params.columnSpec = GridLayout.spec(
                    GridLayout.UNDEFINED,
                    1f
            );

            dayCell.setLayoutParams(params);

            dayCell.setOnClickListener(v -> {

                calendarSelected.set(
                        year,
                        calendarDisplayed.get(Calendar.MONTH),
                        selectedDayOfMonth
                );

                showCalendar();
            });

            gridCalendar.addView(dayCell);
        }

        showCalendarTasks();
    }

    // ADICIONAR ESPAÇO VAZIO NO CALENDÁRIO
    private void addCalendarSpace() {

        TextView space = new TextView(this);

        GridLayout.LayoutParams params =
                new GridLayout.LayoutParams();

        params.width = 0;
        params.height = dp(54);
        params.columnSpec = GridLayout.spec(
                GridLayout.UNDEFINED,
                1f
        );

        space.setLayoutParams(params);

        gridCalendar.addView(space);
    }

    // CRIAR FUNDO CIRCULAR DO DIA SELECIONADO
    private GradientDrawable createCircle(String color) {

        GradientDrawable drawable = new GradientDrawable();

        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(Color.parseColor(color));

        return drawable;
    }

    // VERIFICAR SE EXISTE ATIVIDADE EM UMA DATA
    private boolean hasTasksOnDate(
            int year,
            int month,
            int day
    ) {

        for (int i = 0; i < tasks.length(); i++) {

            try {

                JSONObject task = tasks.getJSONObject(i);

                if (task.getInt("year") == year
                        && task.getInt("month") == month
                        && task.getInt("day") == day) {

                    return true;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    // MOSTRAR ATIVIDADES DO DIA SELECIONADO
    private void showCalendarTasks() {

        layoutCalendarTasks.removeAllViews();

        int year = calendarSelected.get(Calendar.YEAR);
        int month = calendarSelected.get(Calendar.MONTH);
        int day = calendarSelected.get(Calendar.DAY_OF_MONTH);

        String date = String.format(
                Locale.getDefault(),
                "%02d/%02d/%04d",
                day,
                month + 1,
                year
        );

        txtCalendarSelected.setText(
                "Atividades de " + date
        );

        boolean found = false;

        for (int i = 0; i < tasks.length(); i++) {

            try {

                JSONObject task = tasks.getJSONObject(i);

                if (task.getInt("year") == year
                        && task.getInt("month") == month
                        && task.getInt("day") == day) {

                    found = true;

                    String name = task.getString("name");

                    int hour = task.getInt("hour");
                    int minute = task.getInt("minute");

                    boolean completed =
                            task.getBoolean("completed");

                    String time = String.format(
                            Locale.getDefault(),
                            "%02d:%02d",
                            hour,
                            minute
                    );

                    TextView taskText = new TextView(this);

                    String status = completed ? " ✓" : "";

                    taskText.setText(
                            "• " + time + " - " + name + status
                    );

                    taskText.setTextSize(14);
                    taskText.setTextColor(Color.DKGRAY);
                    taskText.setPadding(
                            dp(10),
                            dp(8),
                            dp(10),
                            dp(8)
                    );

                    layoutCalendarTasks.addView(taskText);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (!found) {

            TextView emptyText = new TextView(this);

            emptyText.setText(
                    "Nenhuma atividade cadastrada para este dia."
            );

            emptyText.setTextSize(13);
            emptyText.setTextColor(Color.GRAY);
            emptyText.setPadding(
                    dp(10),
                    dp(8),
                    dp(10),
                    dp(8)
            );

            layoutCalendarTasks.addView(emptyText);
        }
    }

    // CONVERTER DP PARA PIXELS
    private int dp(int value) {

        return (int) (
                value * getResources()
                        .getDisplayMetrics().density
        );
    }

    // TROCAR HORÁRIOS DE DUAS ATIVIDADES
    private void swapTimes(int first, int second) {

        try {

            JSONObject task1 = tasks.getJSONObject(first);
            JSONObject task2 = tasks.getJSONObject(second);

            int hour1 = task1.getInt("hour");
            int minute1 = task1.getInt("minute");

            int hour2 = task2.getInt("hour");
            int minute2 = task2.getInt("minute");

            task1.put("hour", hour2);
            task1.put("minute", minute2);

            task2.put("hour", hour1);
            task2.put("minute", minute1);

            cancelReminder(task1);
            cancelReminder(task2);

            scheduleReminder(task1);
            scheduleReminder(task2);

            saveTasks();

            showTasks();
            showCalendar();

            Toast.makeText(
                    this,
                    "Horários reorganizados! 💜",
                    Toast.LENGTH_SHORT
            ).show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // PROGRAMAR NOTIFICAÇÃO
    private void scheduleReminder(JSONObject task) {

        try {

            if (task.getBoolean("completed")) {
                return;
            }

            Calendar calendar = Calendar.getInstance();

            calendar.set(
                    task.getInt("year"),
                    task.getInt("month"),
                    task.getInt("day"),
                    task.getInt("hour"),
                    task.getInt("minute"),
                    0
            );

            calendar.set(Calendar.MILLISECOND, 0);

            if (calendar.getTimeInMillis()
                    <= System.currentTimeMillis()) {
                return;
            }

            String taskId = task.getString("id");

            Intent intent = new Intent(
                    this,
                    TaskReminderReceiver.class
            );

            intent.putExtra(
                    "taskName",
                    task.getString("name")
            );

            PendingIntent pendingIntent =
                    PendingIntent.getBroadcast(
                            this,
                            taskId.hashCode(),
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT
                                    | PendingIntent.FLAG_IMMUTABLE
                    );

            AlarmManager alarmManager =
                    (AlarmManager) getSystemService(
                            Context.ALARM_SERVICE
                    );

            if (alarmManager != null) {

                alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // CANCELAR NOTIFICAÇÃO
    private void cancelReminder(JSONObject task) {

        try {

            Intent intent = new Intent(
                    this,
                    TaskReminderReceiver.class
            );

            PendingIntent pendingIntent =
                    PendingIntent.getBroadcast(
                            this,
                            task.getString("id").hashCode(),
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT
                                    | PendingIntent.FLAG_IMMUTABLE
                    );

            AlarmManager alarmManager =
                    (AlarmManager) getSystemService(
                            Context.ALARM_SERVICE
                    );

            if (alarmManager != null) {
                alarmManager.cancel(pendingIntent);
            }

            pendingIntent.cancel();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // SALVAR ATIVIDADES
    private void saveTasks() {

        getSharedPreferences(
                "LembraeData",
                MODE_PRIVATE
        ).edit()
                .putString("tasks", tasks.toString())
                .apply();
    }

    // CARREGAR ATIVIDADES SALVAS
    private void loadTasks() {

        String savedTasks = getSharedPreferences(
                "LembraeData",
                MODE_PRIVATE
        ).getString("tasks", "[]");

        try {

            tasks = new JSONArray(savedTasks);

            Calendar today = Calendar.getInstance();

            for (int i = 0; i < tasks.length(); i++) {

                JSONObject task = tasks.getJSONObject(i);

                if (!task.has("id")) {
                    task.put(
                            "id",
                            String.valueOf(
                                    System.currentTimeMillis() + i
                            )
                    );
                }

                if (!task.has("hour")) {
                    task.put("hour", 23);
                }

                if (!task.has("minute")) {
                    task.put("minute", 59);
                }

                if (!task.has("year")) {
                    task.put("year", today.get(Calendar.YEAR));
                }

                if (!task.has("month")) {
                    task.put("month", today.get(Calendar.MONTH));
                }

                if (!task.has("day")) {
                    task.put("day", today.get(Calendar.DAY_OF_MONTH));
                }
            }

            saveTasks();

        } catch (Exception e) {
            tasks = new JSONArray();
        }
    }
}
