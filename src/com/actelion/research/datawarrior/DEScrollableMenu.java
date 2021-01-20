/**
 * This class was found in the Thread
 * http://forum.java.sun.com/thread.jspa?forumID=57&threadID=123183
 *
 * I tried to contact the Author, without any luck. If you are the Author and
 * don't like the Usage of your Code in this Project or want to be named, please
 * mail us!
 */
package com.actelion.research.datawarrior;
 
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;
 
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.basic.BasicPopupMenuUI;
 
// This class implements a scrollable JMenu
// This class was hacked out in a couple of hours,
// You can change maxItemsToDisplay to whatever you want, I used 25 and reduced
// it to fit into the screen
// I have NOT tested this class very much to see if it loses items, etc, ***USE
// AT YOUR OWN RISK***
// Feel free to use and modify (Please add bug fixes here).
//
// This class should only be used until SUN makes a real scrollable JMenu
 
/**
 * An implementation of a scrollable menu -- a popup window containing
 * <code>JMenuItem</code>s that is displayed when the user selects an item on
 * the <code>JMenuBar</code>. In addition to <code>JMenuItem</code>s, a
 * <code>JMenu</code> can also contain <code>JSeparator</code>s.
 * <p>
 * In essence, a menu is a button with an associated <code>JPopupMenu</code>.
 * When the "button" is pressed, the <code>JPopupMenu</code> appears. If the
 * "button" is on the <code>JMenuBar</code>, the menu is a top-level window.
 * If the "button" is another menu item, then the <code>JPopupMenu</code> is
 * "pull-right" menu.
 *
 * If the menu contains more items than displayable on the screen the menu
 * becomes scrollable by hiding some of the items and adding an add and a down
 * arrow at both ends of the menu to scroll the menu with this arrows.
 *
 * description: A popup window containing menu items displayed in a menu bar.
 *
 * @see JPopupMenu
 */
public class DEScrollableMenu extends JMenu {
    private static final long serialVersionUID = 0x20120711;
 
    private int maxItemsToDisplay = 1;
 
    private enum ScrollDirection {
    	UP, DOWN
    	}
 
  static {
    // put a wrapper action between up and down selection action to scroll up or
    // down
    JPopupMenu dummy = new JPopupMenu();
    BasicPopupMenuUI ui = (BasicPopupMenuUI) BasicPopupMenuUI.createUI(dummy);
    ui.installUI(dummy); // create action map
 
    ActionMap map = (ActionMap) UIManager.getLookAndFeelDefaults().get("PopupMenu.actionMap");
 
    if (map != null) {
      Action downAction = map.get("selectNext");
      Action upAction = map.get("selectPrevious");
 
      map.put("selectNext", new SelectNextItemAction(ScrollDirection.DOWN, downAction));
      map.put("selectPrevious", new SelectNextItemAction(ScrollDirection.UP, upAction));
    }
  }
 
  private void setMaxItemToDisplay() {
    // set max items count visible on screen
    Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
    maxItemsToDisplay = (dim.height / maxHeight) - 3;
  }
 
  private static class SelectNextItemAction extends AbstractAction {
	private static final long serialVersionUID = 20140219L;

	private ScrollDirection direction;
 
    private Action wrappedAction;
 
    SelectNextItemAction(ScrollDirection direction, Action wrappedAction) {
      this.direction = direction;
      this.wrappedAction = wrappedAction;
    }
 
    public void actionPerformed(ActionEvent e) {
 
      MenuSelectionManager msm = MenuSelectionManager.defaultManager();
      MenuElement[] path = msm.getSelectedPath();
      int len = path.length;
 
      if (len > 2 && path[len - 3] instanceof DEScrollableMenu && path[len - 2] instanceof JPopupMenu) {
 
    	  DEScrollableMenu menu = (DEScrollableMenu) path[len - 3];
        MenuElement selected = path[len - 1];
        Component component = (direction == ScrollDirection.UP ? menu
            .getFirstVisibleAndEnabledComponent() : menu
            .getLastVisibleAndEnabledComponent());
        if (component == null || selected == component) {
          boolean enableScroll = (direction == ScrollDirection.UP ? menu.scrollUp.mEnableScroll
              : menu.scrollDown.mEnableScroll);
          if (enableScroll) {
            do {
              if (direction == ScrollDirection.UP) {
                menu.scrollUpClicked();
                component = menu.getFirstVisibleComponent();
                enableScroll = menu.scrollUp.mEnableScroll;
              } else {
                menu.scrollDownClicked();
                component = menu.getLastVisibleComponent();
                enableScroll = menu.scrollDown.mEnableScroll;
              }
            } while (component != null && (!(component instanceof MenuElement)) && (component instanceof JSeparator)
                && enableScroll);
 
            if (component == null || !component.isEnabled()
                || (!(component instanceof MenuElement))) {
              return;
            }
          } else {
            for (int index = 0; index < menu.getMenuComponentCount(); index++) {
              if (direction == ScrollDirection.UP) {
                // very first - scroll to end
                menu.scrollDownClicked();
              } else {
                // very last - scroll to begin
                menu.scrollUpClicked();
              }
            }
          }
        }
      }
 
      wrappedAction.actionPerformed(e);
    }
  }
 
  private ScrollUpOrDownButtonItem scrollUp = new ScrollUpOrDownButtonItem(
      ScrollDirection.UP);
 
  private ScrollUpOrDownButtonItem scrollDown = new ScrollUpOrDownButtonItem(
      ScrollDirection.DOWN);
 
  private JSeparator upSeperator = new JSeparator();
 
  private JSeparator downSeperator = new JSeparator();
 
  private Vector<Component> scrollableItems = new Vector<Component>();
 
  private int beginIndex = 0;
 
  private int maxWidth = 10;
 
  private int maxHeight = 1;
 
  /**
   * Constructs a new <code>JMenu</code> with no text.
   */
  public DEScrollableMenu() {
    this("");
  }
 
  /**
   * Constructs a new <code>JMenu</code> whose properties are taken from the
   * <code>Action</code> supplied.
   *
   * @param a an <code>Action</code>
   *
   * @since 1.3
   */
  public DEScrollableMenu(Action a) {
    this("");
    setAction(a);
  }
 
  /**
   * Constructs a new <code>JMenu</code> with the supplied string as its text
   * and specified as a tear-off menu or not.
   *
   * @param s the text for the menu label
   * @param b can the menu be torn off (not yet implemented)
   */
  public DEScrollableMenu(String s, boolean b) {
    this(s);
  }
 
  /**
   * Constructs a new <code>JMenu</code> with the supplied string as its text.
   *
   * @param menuTitle the text for the menu label
   */
  public DEScrollableMenu(String menuTitle) {
    super(menuTitle);
 
    super.add(scrollUp);
    super.add(upSeperator);
    super.add(downSeperator);
    super.add(scrollDown);
 
    getPopupMenu().addPopupMenuListener(new PopupMenuListener() {
      public void popupMenuCanceled(PopupMenuEvent e) {}
      public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
 
      public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        // reset the preferred size, because otherwise the height will not change after items have been added
        getPopupMenu().setPreferredSize(null);
        getPopupMenu().setPreferredSize(new Dimension(maxWidth, getPopupMenu().getPreferredSize().height));
      }
    });
  }
 
  /**
   * Appends a menu item to the end of this menu. Returns the menu item added.
   *
   * @param menuItem the <code>JMenuitem</code> to be added
   * @return the <code>JMenuItem</code> added
   */
  public JMenuItem add(JMenuItem menuItem) {
    addScrollableComponent(menuItem);
    return menuItem;
  }
 
  /**
   * Appends a component to the end of this menu. Returns the component added.
   *
   * @param component the <code>Component</code> to add
   * @return the <code>Component</code> added
   */
  public Component add(Component component) {
    addScrollableComponent(component);
    return component;
  }
 
  /**
   * Adds the specified component to this container at the given position. If
   * <code>index</code> equals -1, the component will be appended to the end.
   *
   * @param component the <code>Component</code> to add
   * @param index the position at which to insert the component
   * @return the <code>Component</code> added
   * @see #remove(Component)
   * @see java.awt.Container#add(Component, int)
   */
  public Component add(Component component, int index) {
    addScrollableComponent(component, index);
    return component;
  }
 
  public void insert(String s, int pos) {
    if (pos < 0) {
      throw new IllegalArgumentException("index less than zero.");
    }
 
    insert(new JMenuItem(s), pos);
 
  }
 
  /**
   * Inserts the specified <code>JMenuitem</code> at a given position.
   *
   * @param menuItem the <code>JMenuitem</code> to add
   * @param pos an integer specifying the position at which to add the new
   *          <code>JMenuitem</code>
   * @return the new menu item
   * @exception IllegalArgumentException if the value of <code>pos</code> < 0
   */
  public JMenuItem insert(JMenuItem menuItem, int pos) {
    if (pos < 0) {
      throw new IllegalArgumentException("index less than zero.");
    }
 
    addScrollableComponent(menuItem, pos);
    return menuItem;
  }
 
  /**
   * Inserts a new menu item attached to the specified <code>Action</code>
   * object at a given position.
   *
   * @param a the <code>Action</code> object for the menu item to add
   * @param pos an integer specifying the position at which to add the new menu
   *          item
   * @exception IllegalArgumentException if the value of <code>pos</code> < 0
   */
  public JMenuItem insert(Action a, int pos) {
    if (pos < 0) {
      throw new IllegalArgumentException("index less than zero.");
    }
 
    JMenuItem menuItem = new JMenuItem((String) a.getValue(Action.NAME), (Icon) a.getValue(Action.SMALL_ICON));
    menuItem.setHorizontalTextPosition(SwingConstants.TRAILING);
    menuItem.setVerticalTextPosition(SwingConstants.CENTER);
    menuItem.setEnabled(a.isEnabled());
    menuItem.setAction(a);
    insert(menuItem, pos);
 
    return menuItem;
  }
 
  /**
   * Returns the <code>JMenuItem</code> at the specified position. If the
   * component at <code>pos</code> is not a menu item, <code>null</code> is
   * returned. This method is included for AWT compatibility.
   *
   * @param pos an integer specifying the position
   * @exception IllegalArgumentException if the value of <code>pos</code> < 0
   * @return the menu item at the specified position; or <code>null</code> if
   *         the item as the specified position is not a menu item
   */
  public JMenuItem getItem(int pos) {
    if (pos < 0) {
      throw new IllegalArgumentException("index less than zero.");
    }
 
    JMenuItem menuItem = null;
 
    Component component = getMenuComponent(pos);
    if (component instanceof JMenuItem) {
      menuItem = (JMenuItem) component;
    }
 
    return menuItem;
  }
 
  /**
   * Returns the number of items on the menu, including separators. This method
   * is included for AWT compatibility.
   *
   * @return an integer equal to the number of items on the menu
   * @see #getMenuComponentCount
   */
  public int getItemCount() {
    return getMenuComponentCount();
  }
 
  /**
   * Removes the specified menu item from this menu. If there is no popup menu,
   * this method will have no effect.
   *
   * @param menuItem the <code>JMenuItem</code> to be removed from the menu
   */
  public void remove(JMenuItem menuItem) {
    removeScrollableComponent(menuItem);
  }
 
  /**
   * Removes the menu item at the specified index from this menu.
   *
   * @param pos the position of the item to be removed
   * @exception IllegalArgumentException if the value of <code>pos</code> < 0,
   *              or if <code>pos</code> is greater than the number of menu
   *              items
   */
  public void remove(int pos) {
    if (pos < 0) {
      throw new IllegalArgumentException("index less than zero.");
    }
    if (pos > getItemCount()) {
      throw new IllegalArgumentException("index greater than the number of items.");
    }
    removeScrollableComponent(scrollableItems.elementAt(pos));
  }
 
  /**
   * Removes the component <code>c</code> from this menu.
   *
   * @param component the component to be removed
   */
  public void remove(Component component) {
    removeScrollableComponent(component);
  }
 
  /**
   * Removes all menu items from this menu.
   */
  public void removeAll() {
    while (getMenuComponentCount() > 0) {
      remove(0);
    }
    maxWidth = 10;
    maxHeight = 0;
  }
 
  /**
   * Returns the number of components on the menu.
   *
   * @return an integer containing the number of components on the menu
   */
  public int getMenuComponentCount() {
    return scrollableItems.size();
  }
 
  /**
   * Returns the component at position <code>n</code>.
   *
   * @param n the position of the component to be returned
   * @return the component requested, or <code>null</code> if there is no
   *         popup menu
   *
   */
  public Component getMenuComponent(int n) {
    if (n >= 0 && n < scrollableItems.size()) {
      return scrollableItems.elementAt(n);
    }
    return null;
  }
 
  /**
   * Returns an array of <code>Component</code>s of the menu's subcomponents.
   * Note that this returns all <code>Component</code>s in the popup menu,
   * including separators.
   *
   * @return an array of <code>Component</code>s or an empty array if there
   *         is no popup menu
   */
  public Component[] getMenuComponents() {
    Component[] components = new Component[getMenuComponentCount()];
    Iterator<Component> iterator = scrollableItems.iterator();
    int index = 0;
    while (iterator.hasNext()) {
      components[index++] = iterator.next();
    }
    return components;
  }
 
  /**
   * Returns true if the specified component exists in the submenu hierarchy.
   *
   * @param component the <code>Component</code> to be tested
   * @return true if the <code>Component</code> exists, false otherwise
   */
  public boolean isMenuComponent(Component component) {
    return scrollableItems.contains(component);
  }
 
  /**
   * Appends a new separator to the end of the menu.
   */
  public void addSeparator() {
    add(new JPopupMenu.Separator());
  }
 
  /**
   * Add the specified component to this scrollable menu
   *
   * @param component the <code>Component</code> to add
   * @param pos an integer specifying the position at which to add the new
   *          component
   */
  protected void addScrollableComponent(Component component, int pos) {
 
    if (pos < 0) {
      throw new IllegalArgumentException("index less than zero.");
    }
 
    scrollableItems.insertElementAt(component, pos);
 
    setPreferedSizeForMenuItems(component);
 
    if (pos >= beginIndex && pos < beginIndex + maxItemsToDisplay) {
      super.add(component, pos - beginIndex + 2);
    }
 
    while(super.getMenuComponentCount() > maxItemsToDisplay + 4) {
      super.remove(super.getMenuComponentCount() - 3);
    }
 
    updateScrollingComponentsVisibility();
  }
 
  /**
   * Add the specified component at the end of this scrollable menu
   *
   * @param component the <code>Component</code> to add
   */
  protected void addScrollableComponent(Component component) {
    addScrollableComponent(component, scrollableItems.size());
  }
 
  /**
   * Remove the specified component from this scrollable menu
   *
   * @param component the <code>Component</code> to remove
   */
  protected void removeScrollableComponent(Component component) {
 
    scrollableItems.remove(component);
    super.remove(component);
 
    if (scrollableItems.size() > maxItemsToDisplay && super.getMenuComponentCount() - 4 < maxItemsToDisplay) {
 
      if (beginIndex + maxItemsToDisplay <= scrollableItems.size()) {
        int end = beginIndex + maxItemsToDisplay - 1;
        Component addComponent = scrollableItems.elementAt(end);
 
        super.add(addComponent, maxItemsToDisplay + 1);
      } else if (beginIndex > 0 && beginIndex <= scrollableItems.size()) {
 
        Component addComponent = scrollableItems.elementAt(--beginIndex);
 
        super.add(addComponent, 2);
      }
    } else if (beginIndex > 0 && beginIndex + maxItemsToDisplay > scrollableItems.size()) {
      beginIndex--;
    }
 
    updateScrollingComponentsVisibility();
  }
 
  private Component getFirstVisibleAndEnabledComponent() {
    if (super.getMenuComponentCount() > 4) {
      for (int index = 2; index < super.getMenuComponentCount() - 2; index++) {
        Component component = super.getMenuComponent(index);
        if (component instanceof MenuElement && component.isEnabled()) {
          return component;
        }
      }
    }
    return null;
  }
 
  private Component getLastVisibleAndEnabledComponent() {
    if (super.getMenuComponentCount() > 4) {
      for (int index = super.getMenuComponentCount() - 3; index > 1; index--) {
        Component component = super.getMenuComponent(index);
 
        if (component instanceof MenuElement && component.isEnabled()) {
          return component;
        }
      }
    }
    return null;
  }
 
  private Component getFirstVisibleComponent() {
    if (super.getMenuComponentCount() > 4) {
      return super.getMenuComponent(2);
    }
    return null;
  }
 
  private Component getLastVisibleComponent() {
    if (super.getMenuComponentCount() > 4) {
      return super.getMenuComponent(super.getMenuComponentCount() - 3);
    }
    return null;
  }
 
  private void updateScrollingComponentsVisibility() {
    boolean visible = scrollableItems.size() > maxItemsToDisplay;
    scrollDown.setVisible(visible);
    scrollUp.setVisible(visible);
    upSeperator.setVisible(visible);
    downSeperator.setVisible(visible);
 
    if (visible) {
      scrollUp.enableScroll(beginIndex > 0);
      scrollDown.enableScroll(beginIndex + maxItemsToDisplay < scrollableItems.size());
    }
 
    getPopupMenu().validate();
    getPopupMenu().repaint();
  }
 
  private void setPreferedSizeForMenuItems(Component component) {
    if (component instanceof JComponent && !(component instanceof JPopupMenu.Separator)) {
      JComponent jcomp = (JComponent) component;
 
      int width = jcomp.getPreferredSize().width;
      int height = jcomp.getPreferredSize().height;
 
      if (jcomp.getBorder() != null) {
        Insets insets = jcomp.getBorder().getBorderInsets(component);
        width += insets.left + insets.right;
      }
      if (width > maxWidth || height > maxHeight) {
        if (width > maxWidth) {
          maxWidth = width;
        }
        if (height > maxHeight) {
          maxHeight = height;
          setMaxItemToDisplay();
        }
 
        for (Component scrollableItem : scrollableItems) {
          if (scrollableItem instanceof JComponent && !(scrollableItem instanceof JPopupMenu.Separator)) {
            JComponent jComponent = (JComponent) scrollableItem;
            jComponent.setPreferredSize(new Dimension(maxWidth, maxHeight));
          }
        }
      } else {
        jcomp.setPreferredSize(new Dimension(maxWidth, maxHeight));
      }
    }
  }
 
  private void scrollUpClicked() {
    if (scrollableItems.size() <= maxItemsToDisplay || beginIndex == 0) {
      // no need to scroll
      return;
    }
 
    super.remove(maxItemsToDisplay + 1);
    super.add(scrollableItems.elementAt(--beginIndex), 2);
 
    updateScrollingComponentsVisibility();
 
    if (getLastVisibleComponent() instanceof JSeparator) {
      scrollUpClicked();
    }
  }
 
  private void scrollDownClicked() {
    if (scrollableItems.size() <= maxItemsToDisplay
        || beginIndex + maxItemsToDisplay == scrollableItems.size()) {
      // no need to scroll
      return;
    }
 
    super.remove(2);
 
    super.add(scrollableItems.elementAt(beginIndex + maxItemsToDisplay), maxItemsToDisplay + 1);
    beginIndex++;
 
    updateScrollingComponentsVisibility();
 
    if (getFirstVisibleComponent() instanceof JSeparator) {
      scrollDownClicked();
    }
  }
 
  private class ScrollUpOrDownButtonItem extends JMenuItem {
	private static final long serialVersionUID = 20140219L;
 
    private ScrollDirection mDirection = ScrollDirection.UP;
 
    private Polygon mArrow = null;
 
    private boolean mIsMouseOver = false;
 
    private boolean mEnableScroll = false;
 
    private MyMouseListener mMouseListener;
 
    private MyActionListener mActionListener;
 
    private int mInitialDelay = 300;
 
    private int mRepeatDelay = 50;
 
    private Timer mTimer = null;
 
    public ScrollUpOrDownButtonItem(ScrollDirection direction) {
      this.mDirection = direction;
      setVisible(false);
 
      setPreferredSize(new Dimension(10, 10));
      setSize(new Dimension(10, 10));
      setMinimumSize(new Dimension(10, 10));
 
      mMouseListener = new MyMouseListener();
      addMouseListener(mMouseListener);
 
      mActionListener = new MyActionListener();
      mTimer = new Timer(mRepeatDelay, mActionListener);
      mTimer.setInitialDelay(mInitialDelay);
    }
 
    public void enableScroll(boolean enableScroll) {
      this.mEnableScroll = enableScroll;
      repaint();
    }
 
    public void paintComponent(Graphics g) {
 
      Color oldColor = g.getColor();
 
      g.setColor(DEScrollableMenu.this.getBackground());
      Rectangle rect = g.getClipBounds();
      g.fillRect(rect.x, rect.y, rect.width, rect.height);
 
      if (mIsMouseOver && mEnableScroll) {
        g.setColor(Color.blue);
      } else if (!mEnableScroll) {
        g.setColor(Color.gray);
      } else {
        g.setColor(DEScrollableMenu.this.getForeground());
      }
 
      g.fillPolygon(getArrow());
      g.setColor(oldColor);
    }
 
    private Polygon getArrow() {
      if (mArrow == null) {
        mArrow = new Polygon();
        if (mDirection == ScrollDirection.UP) {
          mArrow.addPoint((int) (getSize().width / 2.0 - 6.0 + 0.5), (int) (getSize().height / 2.0 + 3.0 + 0.5));
          mArrow.addPoint((int) (getSize().width / 2.0 + 6.0 + 0.5), (int) (getSize().height / 2.0 + 3.0 + 0.5));
          mArrow.addPoint((int) (getSize().width / 2.0 + 0.5), (int) (getSize().height / 2.0 - 4.0 + 0.5));
        } else {
          mArrow.addPoint((int) (getSize().width / 2.0 - 6.0 + 0.5), (int) (getSize().height / 2.0 - 3.0 + 0.5));
          mArrow.addPoint((int) (getSize().width / 2.0 + 6.0 + 0.5), (int) (getSize().height / 2.0 - 3.0 + 0.5));
          mArrow.addPoint((int) (getSize().width / 2.0 + 0.5), (int) (getSize().height / 2.0 + 4.0 + 0.5));
        }
      }
      return mArrow;
    }
 
    private void scroll() {
      if (mDirection == ScrollDirection.UP) {
        scrollUpClicked();
      } else {
        scrollDownClicked();
      }
    }
 
    private void startScrollTimer() {
      if (mEnableScroll) {
        mTimer.start();
      } else {
        mTimer.stop();
      }
    }
 
    /**
     * action for timer
     */
    private class MyActionListener implements ActionListener {
 
      public void actionPerformed(ActionEvent actionevent) {
        scroll();
      }
    }
 
    private class MyMouseListener extends MouseAdapter {
 
      public void mouseClicked(MouseEvent me) {
        scroll();
      }
 
      public void mouseEntered(MouseEvent me) {
        mIsMouseOver = true;
        repaint();
        startScrollTimer();
      }
 
      public void mouseExited(MouseEvent me) {
        mIsMouseOver = false;
        mTimer.stop();
        repaint();
      }
 
      public void mousePressed(MouseEvent mouseEvent) {
        startScrollTimer();
      }
 
      public void mouseReleased(MouseEvent mouseevent) {
        mTimer.stop();
      }
    }
  }
 
  public MenuElement[] getSubElements() {
    ArrayList<MenuElement> elements = new ArrayList<MenuElement>();
 
    for(Component c : scrollableItems) {
      if(c instanceof MenuElement) {
        elements.add((MenuElement)c);
      }
    }
 
    return elements.toArray(new MenuElement[elements.size()]);
  }
}