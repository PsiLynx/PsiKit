// Copyright (c) 2021-2025 Littleton Robotics
// http://github.com/Mechanical-Advantage
//
// Use of this source code is governed by a BSD
// license that can be found in the LICENSE file
// at the root directory of this project.

package org.psilynx.psikit.core.mechanism;

import org.psilynx.psikit.core.LogTable;
import org.psilynx.psikit.core.wpi.Color8Bit;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Visual 2D representation of arms, elevators, and general mechanisms through a node-based API.
 *
 * <p>A Mechanism2d object is published and contains at least one root node. A root is the anchor
 * point of other nodes (such as ligaments). Other nodes are recursively appended based on other
 * nodes.
 *
 */
public final class LoggedMechanism2d {
  private final Map<String, LoggedMechanismRoot2d> m_roots;
  private final double[] m_dims = new double[2];
  private String m_color;

  /**
   * Create a new Mechanism2d with the given dimensions and default color (dark blue).
   *
   * <p>The dimensions represent the canvas that all the nodes are drawn on.
   *
   * @param width the width
   * @param height the height
   */
  public LoggedMechanism2d(double width, double height) {
    this(width, height, new Color8Bit(0, 0, 32));
  }

  /**
   * Create a new Mechanism2d with the given dimensions.
   *
   * <p>The dimensions represent the canvas that all the nodes are drawn on.
   *
   * @param width the width
   * @param height the height
   * @param backgroundColor the background color. Defaults to dark blue.
   */
  public LoggedMechanism2d(double width, double height, Color8Bit backgroundColor) {
    m_roots = new HashMap<>();
    m_dims[0] = width;
    m_dims[1] = height;
    setBackgroundColor(backgroundColor);
  }

  public void close() {
    for (LoggedMechanismRoot2d root : m_roots.values()) {
      root.close();
    }
  }

  /**
   * Get or create a root in this Mechanism2d with the given name and position.
   *
   * <p>If a root with the given name already exists, the given x and y coordinates are not used.
   *
   * @param name the root name
   * @param x the root x coordinate
   * @param y the root y coordinate
   * @return a new root joint object, or the existing one with the given name.
   */
  public synchronized LoggedMechanismRoot2d getRoot(String name, double x, double y) {
    LoggedMechanismRoot2d existing = m_roots.get(name);
    if (existing != null) {
      return existing;
    }

    LoggedMechanismRoot2d root = new LoggedMechanismRoot2d(name, x, y);
    m_roots.put(name, root);
    return root;
  }

  public synchronized LoggedMechanismRoot2d getRoot() {
    return m_roots.values().iterator().next();
  }

  /**
   * Set the Mechanism2d background color.
   *
   * @param color the new color
   */
  public synchronized void setBackgroundColor(Color8Bit color) {
    m_color = color.toHexString();
  }

  public synchronized void logOutput(LogTable table) {
    table.put(".type", "Mechanism2d");
    table.put(".controllable", false);
    table.put("dims", m_dims);
    table.put("backgroundColor", m_color);
    for (Entry<String, LoggedMechanismRoot2d> entry : m_roots.entrySet()) {
      String name = entry.getKey();
      LoggedMechanismRoot2d root = entry.getValue();
      synchronized (root) {
        root.logOutput(table.getSubtable(name));
      }
    }
  }
}
