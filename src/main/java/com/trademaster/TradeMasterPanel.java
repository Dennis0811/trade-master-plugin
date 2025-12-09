package com.trademaster;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;

@Slf4j
public class TradeMasterPanel extends PluginPanel {
    public TradeMasterPanel() {
        final int Y_GAP = 5;
        final int X_GAP = Y_GAP * 2;
        final int MARGIN = 15;

        this.setBorder(BorderFactory.createEmptyBorder(MARGIN, MARGIN, MARGIN, MARGIN));


        ImageIcon scaledHomeIcon = getScaledImageIcon("/home_icon_white_filled.png", 20, 20);
        JButton homeButton = new JButton(scaledHomeIcon);
        homeButton.setToolTipText("Home");
        homeButton.setMargin(new Insets(Y_GAP, X_GAP, Y_GAP, X_GAP));

        ImageIcon scaledGpIcon = getScaledImageIcon("/gp_icon.png", 20, 20);
        JLabel gpLabel = new JLabel(scaledGpIcon);
        gpLabel.setToolTipText("Gold Pieces (GP)");

        long PLAYER_WEALTH = 135884962L;
        JLabel wealthLabel = new JLabel(abbreviateNumber(PLAYER_WEALTH) + " GP");
        wealthLabel.setToolTipText("Bank: " + formatNumber(5232) + " GP\nInventory: " + formatNumber(1356) + " GP\nTotal: " + formatNumber(PLAYER_WEALTH) + " GP");

        JPanel wealthGroup = new JPanel();
        wealthGroup.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, Y_GAP));
        wealthGroup.add(gpLabel);
        wealthGroup.add(wealthLabel);

        JPanel homeWealthGroup = new JPanel();
        homeWealthGroup.setLayout(new BoxLayout(homeWealthGroup, BoxLayout.X_AXIS));
        homeWealthGroup.add(homeButton);
        homeWealthGroup.add(Box.createHorizontalGlue());
        homeWealthGroup.add(wealthGroup);


        ImageIcon watchlistIcon = getScaledImageIcon("/bookmark_icon_white_filled.png", 20, 20);
        JButton watchlistButton = new JButton("Watchlist", watchlistIcon);
        watchlistButton.setMargin(new Insets(Y_GAP, X_GAP, Y_GAP, X_GAP));
        watchlistButton.setHorizontalAlignment(SwingConstants.LEFT);

        ImageIcon offersIcon = getScaledImageIcon("/ge_icon.png", 20, 20);
        JButton offersButton = new JButton("Offers", offersIcon);
        offersButton.setMargin(new Insets(Y_GAP, X_GAP, Y_GAP, X_GAP));
        offersButton.setHorizontalAlignment(SwingConstants.LEFT);

        ImageIcon tradesIcon = getScaledImageIcon("/ledger_icon.png", 20, 20);
        JButton tradesButton = new JButton("Trade History", tradesIcon);
        tradesButton.setMargin(new Insets(Y_GAP, X_GAP, Y_GAP, X_GAP));
        tradesButton.setHorizontalAlignment(SwingConstants.LEFT);


        JPanel searchPanel = new JPanel();
        searchPanel.setLayout(new BoxLayout(searchPanel, BoxLayout.X_AXIS));
        searchPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        searchPanel.setBackground(new Color(30, 30, 30));

        ImageIcon searchIcon = getScaledImageIcon("/search_icon.png", 14, 14);
        JLabel iconLabel = new JLabel(searchIcon);
        iconLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        iconLabel.setOpaque(false);

        JTextField searchField = new JTextField();
        searchField.setBackground(new Color(30, 30, 30));
        searchField.setForeground(Color.WHITE);
        searchField.setCaretColor(Color.WHITE);
        searchField.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        searchPanel.add(iconLabel);
        searchPanel.add(Box.createHorizontalStrut(5));
        searchPanel.add(searchField);


        JLabel salesTitle = new JLabel("Sales".toUpperCase());
        JLabel salesTotalEarned = new JLabel("Total Earned");
        JLabel salesTotalEarnedValue = new JLabel(abbreviateNumber(87838) + " GP");
        JLabel salesTopItem = new JLabel("Top Item");
        JLabel salesTopItemValue = new JLabel("Logs");

        JPanel salesGroup = new JPanel();
        salesGroup.setLayout(new BoxLayout(salesGroup, BoxLayout.Y_AXIS));

        JPanel salesTitleContainer = new JPanel();
        JPanel salesTotalEarnedContainer = new JPanel();
        JPanel salesTopItemContainer = new JPanel();
        salesTitleContainer.setLayout(new BoxLayout(salesTitleContainer, BoxLayout.X_AXIS));
        salesTotalEarnedContainer.setLayout(new BoxLayout(salesTotalEarnedContainer, BoxLayout.X_AXIS));
        salesTopItemContainer.setLayout(new BoxLayout(salesTopItemContainer, BoxLayout.X_AXIS));

        salesTitleContainer.add(salesTitle);
        salesTitleContainer.add(Box.createHorizontalGlue());
        salesGroup.add(salesTitleContainer);

        salesTotalEarnedContainer.add(salesTotalEarned);
        salesTotalEarnedContainer.add(Box.createHorizontalGlue());
        salesTotalEarnedContainer.add(salesTotalEarnedValue);
        salesGroup.add(salesTotalEarnedContainer);

        salesTopItemContainer.add(salesTopItem);
        salesTopItemContainer.add(Box.createHorizontalGlue());
        salesTopItemContainer.add(salesTopItemValue);
        salesGroup.add(salesTopItemContainer);


        add(homeWealthGroup);
        add(searchPanel);
        add(watchlistButton);
        add(offersButton);
        add(tradesButton);
        add(salesGroup);
    }

    private static String formatNumber(long value) {
        return NumberFormat.getNumberInstance().format(value);
    }

    private static String abbreviateNumber(long value) {
//        only abbreviate numbers over 1 billion (1b = 10^9)
        if (value < Math.pow(10, 9)) return formatNumber(value);

        String[] suffixes = {"", "K", "M", "B", "T", "Qa", "Qi", "Oc"};
        int suffixIndex = 0;
        double dividedValue = value;

        while (dividedValue >= 1000 && suffixIndex < suffixes.length - 1) {
            dividedValue /= 1000;
            suffixIndex++;
        }

        return String.format("%.1f%s", dividedValue, suffixes[suffixIndex]);
    }

    private static ImageIcon getScaledImageIcon(String path, int w, int h) {
        ImageIcon homeIcon = new ImageIcon(TradeMasterPlugin.class.getResource(path));
        Image scaledImage = homeIcon.getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH);
        return new ImageIcon(scaledImage);
    }


}
