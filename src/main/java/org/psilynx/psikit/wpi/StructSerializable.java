// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package org.psilynx.psikit.wpi;


/**
 * Marker interface to indicate a class is serializable using Struct serialization.
 *
 * <p>While this cannot be enforced by the interface, any class implementing this interface should
 * provide a public final static `struct` member variable.
 */
public interface StructSerializable extends WPISerializable {}
