package dev.gracco.ui.element;

import dev.gracco.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class DatePickerPopup extends JDialog {

    private LocalDate selectedDate;
    private YearMonth viewMonth;
    private final Consumer<LocalDate> onSelect;
    private JLabel monthYearLabel;
    private JPanel calendarGrid;

    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("MMMM yyyy");
    private static final String[] DAY_NAMES = {"Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"};

    public DatePickerPopup(Window owner, LocalDate initial, Consumer<LocalDate> onSelect) {
        super(owner, ModalityType.APPLICATION_MODAL);
        this.selectedDate = initial != null ? initial : LocalDate.now();
        this.viewMonth = YearMonth.from(this.selectedDate);
        this.onSelect = onSelect;

        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));

        JPanel root = new JPanel(new BorderLayout(0, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.setColor(Theme.SECONDARY);
                g2.setStroke(new java.awt.BasicStroke(2f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 16, 16);
                g2.dispose();
            }
        };
        root.setOpaque(false);
        root.setBorder(new EmptyBorder(12, 12, 12, 12));
        root.setPreferredSize(new Dimension(280, 280));

        // Header: prev / month-year / next
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, 8, 0));

        JButton prevBtn = navButton("‹");
        JButton nextBtn = navButton("›");

        monthYearLabel = new JLabel("", SwingConstants.CENTER);
        monthYearLabel.setFont(Theme.getFont(Theme.FontType.SEMI_BOLD, 14f));
        monthYearLabel.setForeground(Theme.BLACK);

        prevBtn.addActionListener(e -> { viewMonth = viewMonth.minusMonths(1); rebuildCalendar(); });
        nextBtn.addActionListener(e -> { viewMonth = viewMonth.plusMonths(1); rebuildCalendar(); });

        header.add(prevBtn, BorderLayout.WEST);
        header.add(monthYearLabel, BorderLayout.CENTER);
        header.add(nextBtn, BorderLayout.EAST);

        // Day name row
        JPanel dayNames = new JPanel(new GridLayout(1, 7, 4, 0));
        dayNames.setOpaque(false);
        dayNames.setBorder(new EmptyBorder(0, 0, 4, 0));
        for (String d : DAY_NAMES) {
            JLabel lbl = new JLabel(d, SwingConstants.CENTER);
            lbl.setFont(Theme.getFont(Theme.FontType.MEDIUM, 11f));
            lbl.setForeground(new Color(130, 130, 130));
            dayNames.add(lbl);
        }

        calendarGrid = new JPanel(new GridLayout(0, 7, 4, 4));
        calendarGrid.setOpaque(false);

        JPanel body = new JPanel(new BorderLayout(0, 0));
        body.setOpaque(false);
        body.add(dayNames, BorderLayout.NORTH);
        body.add(calendarGrid, BorderLayout.CENTER);

        root.add(header, BorderLayout.NORTH);
        root.add(body, BorderLayout.CENTER);

        setContentPane(root);
        rebuildCalendar();
        pack();
    }

    private void rebuildCalendar() {
        monthYearLabel.setText(viewMonth.format(DISPLAY_FMT));
        calendarGrid.removeAll();

        LocalDate first = viewMonth.atDay(1);
        int startDow = first.getDayOfWeek().getValue() % 7; // Sun=0

        for (int i = 0; i < startDow; i++) calendarGrid.add(new JLabel());

        for (int day = 1; day <= viewMonth.lengthOfMonth(); day++) {
            LocalDate date = viewMonth.atDay(day);
            boolean isSelected = date.equals(selectedDate);
            boolean isToday = date.equals(LocalDate.now());

            JButton btn = new JButton(String.valueOf(day)) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    if (isSelected) {
                        g2.setColor(Theme.PRIMARY);
                        g2.fillOval(2, 2, getWidth() - 4, getHeight() - 4);
                    } else if (isToday) {
                        g2.setColor(Theme.HIGHLIGHT);
                        g2.fillOval(2, 2, getWidth() - 4, getHeight() - 4);
                    }
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            btn.setFont(Theme.getFont(Theme.FontType.REGULAR, 12f));
            btn.setForeground(isSelected ? Theme.WHITE : Theme.BLACK);
            btn.setContentAreaFilled(false);
            btn.setOpaque(false);
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btn.setMargin(new Insets(0, 0, 0, 0));

            btn.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    if (!isSelected) btn.setForeground(Theme.PRIMARY);
                }
                public void mouseExited(MouseEvent e) {
                    btn.setForeground(isSelected ? Theme.WHITE : Theme.BLACK);
                }
            });

            btn.addActionListener(e -> {
                selectedDate = date;
                if (onSelect != null) onSelect.accept(date);
                dispose();
            });

            calendarGrid.add(btn);
        }

        calendarGrid.revalidate();
        calendarGrid.repaint();
        pack();
    }

    private JButton navButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(Theme.getFont(Theme.FontType.BOLD, 16f));
        btn.setForeground(Theme.PRIMARY);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(32, 28));
        return btn;
    }

    public static void show(Component anchor, LocalDate current, Consumer<LocalDate> onSelect) {
        Window owner = SwingUtilities.getWindowAncestor(anchor);
        DatePickerPopup popup = new DatePickerPopup(owner, current, onSelect);
        Point p = anchor.getLocationOnScreen();
        popup.setLocation(p.x, p.y + anchor.getHeight() + 4);
        popup.setVisible(true);
    }
}
