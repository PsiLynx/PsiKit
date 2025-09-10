// Copyright (c) 2021-2025 Littleton Robotics
// http://github.com/Mechanical-Advantage
//
// Use of this source code is governed by a BSD
// license that can be found in the LICENSE file
// at the root directory of this project.

package org.psilynx.psikit.core.mechanism;

import org.psilynx.psikit.core.LogTable;

import java.util.HashMap;
import java.util.Map;

/**
 * Root Mechanism2d node.
 *
 * <p>A root is the anchor point of other nodes (such as ligaments).
 *
 * <p>Do not create objects of this class directly! Obtain instances from the {@link
 *
 * LoggedMechanism2d#getRoot(String, double, double)}
 * factory method.
 *
 * <p>Append other nodes by using {@link #append(LoggedMechanismObject2d)}.
 */
public final class LoggedMechanismRoot2d implements AutoCloseable {
  private final String m_name;
  private final Map<String, LoggedMechanismObject2d> m_objects = new HashMap<>(1);
  private double m_x;
  private double m_y;

  /**
   * Package-private constructor for roots.
   *
   * @param name name
   * @param x x coordinate of root (provide only when constructing a root node)
   * @param y y coordinate of root (provide only when constructing a root node)
   */
  LoggedMechanismRoot2d(String name, double x, double y) {
    m_name = name;
    m_x = x;
    m_y = y;
  }

  @Override
  public void close() {
    for (LoggedMechanismObject2d obj : m_objects.values()) {
      obj.close();
    }
  }

  /**
   * Append a Mechanism object that is based on this one.
   *
   * @param <T> The object type.
   * @param object the object to add.
   * @return the object given as a parameter, useful for variable assignments and call chaining.
   * @throws UnsupportedOperationException if the object's name is already used - object names must
   *     be unique.
   */
  public synchronized <T extends LoggedMechanismObject2d> T append(T object) {
    if (m_objects.containsKey(object.getName())) {
      throw new UnsupportedOperationException("Mechanism object names must be unique!");
    }
    m_objects.put(object.getName(), object);
    return object;
  }

  public LoggedMechanismObject2d[] objects(){
    return m_objects.values().toArray(new LoggedMechanismObject2d[0]);
  }

  /**
   * Set the root's position.
   *
   * @param x new x coordinate
   * @param y new y coordinate
   */
  public synchronized void setPosition(double x, double y) {
    m_x = x;
    m_y = y;
  }


  public String getName() {
    return m_name;
  }

  synchronized void logOutput(LogTable table) {
    table.put("x", m_x);
    table.put("y", m_y);
    for (LoggedMechanismObject2d obj : m_objects.values()) {
      obj.logOutput(table.getSubtable(obj.getName()));
    }
  }
}
