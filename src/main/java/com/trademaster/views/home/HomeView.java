package com.trademaster.views.home;

import com.trademaster.TradeMasterPlugin;
import com.trademaster.controllers.HomeController;
import com.trademaster.models.WealthSummary;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.IconView;
import java.awt.*;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

public class HomeView extends PluginPanel {
    private final JLabel wealthLabel = new JLabel();
    private final JLabel wealthTooltipLabel = new JLabel();

    public HomeView(HomeController controller) {
        // Home button
        JButton homeButton = new JButton(getScaledIcon("/home_icon_white_filled.png", 20, 20));
        homeButton.setToolTipText("Home");
        homeButton.setMargin(new Insets(0, 0, 0, 0));
        homeButton.setOpaque(false);
        homeButton.setBorderPainted(false);
        homeButton.setContentAreaFilled(false);

        // Wealth Text
        wealthLabel.setIcon(getScaledIcon("/gp_icon.png", 20, 20));

        // Header Row
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.X_AXIS));
        header.add(homeButton);
        header.add(Box.createHorizontalGlue());
        header.add(wealthLabel);
        header.setBorder(new EmptyBorder(5, 0, 5, 0));


        // Search Bar
        IconTextField searchBar = new IconTextField();
        searchBar.setIcon(IconTextField.Icon.SEARCH);
        searchBar.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH, 30));
        searchBar.setBackground(new Color(30, 30, 30));


        // Home Panel Items
        JButton watchlistButton = createPanelButton("/bookmark_icon_white_filled.png", "Watchlist");
        JButton offersButton = createPanelButton("/ge_icon.png", "Offers");
        JButton tradesButton = createPanelButton("/ledger_icon.png", "Trade History");

        // Home Panel Sections
        JPanel salesSection = createHomeSection("Sales", List.of(
                new AbstractMap.SimpleEntry<>("Total Earned", "PLACEHOLDER"),
                new AbstractMap.SimpleEntry<>("Top Item", "PLACEHOLDER")));
        JPanel expensesSection = createHomeSection("Expenses", List.of(
                new AbstractMap.SimpleEntry<>("Total Paid", "PLACEHOLDER"),
                new AbstractMap.SimpleEntry<>("Top Item", "PLACEHOLDER")));
        JPanel profitSection = createHomeSection("Profits", List.of(
                new AbstractMap.SimpleEntry<>("Total Gains", "PLACEHOLDER"),
                new AbstractMap.SimpleEntry<>("Most Profitable Item", "PLACEHOLDER")));


        add(header);
        add(searchBar);
        add(watchlistButton);
        add(offersButton);
        add(tradesButton);
        add(salesSection);
        add(expensesSection);
        add(profitSection);


        controller.setView(this);
        controller.init();
    }

    public void setWealthText(WealthSummary wealth) {
        wealthLabel.setText(wealth.getTotalAbbreviated());
        wealthLabel.setToolTipText("Bank: " + wealth.getBank() + "\n" +
                "Inventory: " + wealth.getInventory() + "\n" +
                "Total: " + wealth.getTotal());
    }

    private ImageIcon getScaledIcon(String path, int w, int h) {
        ImageIcon icon = new ImageIcon(getClass().getResource(path));
        Image scaled = icon.getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    private JButton createPanelButton(String iconPath, String text) {
        JButton button = new JButton(text, getScaledIcon(iconPath, 20, 20));
        button.setHorizontalAlignment(SwingConstants.LEFT);

        return button;
    }

    private JPanel createHomeSection(String title, List<Map.Entry<String, String>> rows) {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));

        JPanel titleContainer = new JPanel();
        titleContainer.setLayout(new BoxLayout(titleContainer, BoxLayout.X_AXIS));
        titleContainer.add(new JLabel(title.toUpperCase()));
        titleContainer.add(Box.createHorizontalGlue());

        section.add(titleContainer);

        for (Map.Entry<String, String> row : rows) {
            JPanel rowPanel = new JPanel();
            JLabel rowTitle = new JLabel(row.getKey());
            JLabel rowValue = new JLabel(row.getValue());

            rowPanel.setLayout(new BoxLayout(rowPanel, BoxLayout.X_AXIS));

            rowPanel.add(rowTitle);
            rowPanel.add(Box.createHorizontalGlue());
            rowPanel.add(rowValue);

            section.add(rowPanel);
        }

        return section;
    }


}
