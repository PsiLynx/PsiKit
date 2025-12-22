// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package org.psilynx.psikit.core.wpi.math;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.psilynx.psikit.core.wpi.StructSerializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;

/** Represents a 3D pose containing translational and rotational elements. */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class Pose3d implements StructSerializable {
  /**
   * A preallocated Pose3d representing the origin.
   *
   * <p>This exists to avoid allocations for common poses.
   */
  public static final Pose3d kZero = new Pose3d();

  private final Translation3d m_translation;
  private final Rotation3d m_rotation;

  /** Constructs a pose at the origin facing toward the positive X axis. */
  public Pose3d() {
    m_translation = Translation3d.kZero;
    m_rotation = Rotation3d.kZero;
  }

  /**
   * Constructs a pose with the specified translation and rotation.
   *
   * @param translation The translational component of the pose.
   * @param rotation The rotational component of the pose.
   */
  @JsonCreator
  public Pose3d(
      @JsonProperty(required = true, value = "translation") Translation3d translation,
      @JsonProperty(required = true, value = "rotation") Rotation3d rotation) {
    m_translation = translation;
    m_rotation = rotation;
  }

  /**
   * Constructs a pose with x, y, and z translations instead of a separate Translation3d.
   *
   * @param x The x component of the translational component of the pose.
   * @param y The y component of the translational component of the pose.
   * @param z The z component of the translational component of the pose.
   * @param rotation The rotational component of the pose.
   */
  public Pose3d(double x, double y, double z, Rotation3d rotation) {
    m_translation = new Translation3d(x, y, z);
    m_rotation = rotation;
  }


  /**
   * Constructs a 3D pose from a 2D pose in the X-Y plane.
   *
   * @param pose The 2D pose.
   * @see Rotation3d#Rotation3d(Rotation2d)
   * @see Translation3d#Translation3d(Translation2d)
   */
  public Pose3d(Pose2d pose) {
    m_translation = new Translation3d(pose.getX(), pose.getY(), 0.0);
    m_rotation = new Rotation3d(0.0, 0.0, pose.getRotation().getRadians());
  }

  /**
   * Transforms the pose by the given transformation and returns the new transformed pose. The
   * transform is applied relative to the pose's frame. Note that this differs from {@link
   * Pose3d#rotateBy(Rotation3d)}, which is applied relative to the global frame and around the
   * origin.
   *
   * @param other The transform to transform the pose by.
   * @return The transformed pose.
   */
  public Pose3d plus(Transform3d other) {
    return transformBy(other);
  }

  /**
   * Returns the Transform3d that maps the one pose to another.
   *
   * @param other The initial pose of the transformation.
   * @return The transform that maps the other pose to the current pose.
   */
  public Transform3d minus(Pose3d other) {
    final var pose = this.relativeTo(other);
    return new Transform3d(pose.getTranslation(), pose.getRotation());
  }

  /**
   * Returns the translation component of the transformation.
   *
   * @return The translational component of the pose.
   */
  @JsonProperty
  public Translation3d getTranslation() {
    return m_translation;
  }

  /**
   * Returns the X component of the pose's translation.
   *
   * @return The x component of the pose's translation.
   */
  public double getX() {
    return m_translation.getX();
  }

  /**
   * Returns the Y component of the pose's translation.
   *
   * @return The y component of the pose's translation.
   */
  public double getY() {
    return m_translation.getY();
  }

  /**
   * Returns the Z component of the pose's translation.
   *
   * @return The z component of the pose's translation.
   */
  public double getZ() {
    return m_translation.getZ();
  }


  /**
   * Returns the rotational component of the transformation.
   *
   * @return The rotational component of the pose.
   */
  @JsonProperty
  public Rotation3d getRotation() {
    return m_rotation;
  }

  /**
   * Multiplies the current pose by a scalar.
   *
   * @param scalar The scalar.
   * @return The new scaled Pose3d.
   */
  public Pose3d times(double scalar) {
    return new Pose3d(m_translation.times(scalar), m_rotation.times(scalar));
  }

  /**
   * Divides the current pose by a scalar.
   *
   * @param scalar The scalar.
   * @return The new scaled Pose3d.
   */
  public Pose3d div(double scalar) {
    return times(1.0 / scalar);
  }

  /**
   * Rotates the pose around the origin and returns the new pose.
   *
   * @param other The rotation to transform the pose by, which is applied extrinsically (from the
   *     global frame).
   * @return The rotated pose.
   */
  public Pose3d rotateBy(Rotation3d other) {
    return new Pose3d(m_translation.rotateBy(other), m_rotation.rotateBy(other));
  }

  /**
   * Transforms the pose by the given transformation and returns the new transformed pose. The
   * transform is applied relative to the pose's frame. Note that this differs from {@link
   * Pose3d#rotateBy(Rotation3d)}, which is applied relative to the global frame and around the
   * origin.
   *
   * @param other The transform to transform the pose by.
   * @return The transformed pose.
   */
  public Pose3d transformBy(Transform3d other) {
    return new Pose3d(
        m_translation.plus(other.getTranslation().rotateBy(m_rotation)),
        other.getRotation().plus(m_rotation));
  }

  /**
   * Returns the current pose relative to the given pose.
   *
   * <p>This function can often be used for trajectory tracking or pose stabilization algorithms to
   * get the error between the reference and the current pose.
   *
   * @param other The pose that is the origin of the new coordinate frame that the current pose will
   *     be converted into.
   * @return The current pose relative to the new origin pose.
   */
  public Pose3d relativeTo(Pose3d other) {
    var transform = new Transform3d(other, this);
    return new Pose3d(transform.getTranslation(), transform.getRotation());
  }

  /**
   * Rotates the current pose around a point in 3D space.
   *
   * @param point The point in 3D space to rotate around.
   * @param rot The rotation to rotate the pose by.
   * @return The new rotated pose.
   */
  public Pose3d rotateAround(Translation3d point, Rotation3d rot) {
    return new Pose3d(m_translation.rotateAround(point, rot), m_rotation.rotateBy(rot));
  }

  /**
   * Returns a Pose2d representing this Pose3d projected into the X-Y plane.
   *
   * @return A Pose2d representing this Pose3d projected into the X-Y plane.
   */
  public Pose2d toPose2d() {
    return new Pose2d(m_translation.toTranslation2d(), m_rotation.toRotation2d());
  }

  /**
   * Returns the nearest Pose3d from a collection of poses. If two or more poses in the collection
   * have the same distance from this pose, return the one with the closest rotation component.
   *
   * @param poses The collection of poses to find the nearest.
   * @return The nearest Pose3d from the collection.
   */
  public Pose3d nearest(Collection<Pose3d> poses) {
    return Collections.min(
        poses,
        Comparator.comparing(
                (Pose3d other) -> this.getTranslation().getDistance(other.getTranslation()))
            .thenComparing(
                (Pose3d other) -> this.getRotation().minus(other.getRotation()).getAngle()));
  }

  @Override
  public String toString() {
    return String.format("Pose3d(%s, %s)", m_translation, m_rotation);
  }

  /**
   * Checks equality between this Pose3d and another object.
   *
   * @param obj The other object.
   * @return Whether the two objects are equal or not.
   */
  @Override
  public boolean equals(Object obj) {
    return obj instanceof Pose3d
        && m_translation.equals(((Pose3d) obj).m_translation)
        && m_rotation.equals(((Pose3d) obj).m_rotation);
  }

  @Override
  public int hashCode() {
    return Objects.hash(m_translation, m_rotation);
  }

  /** Pose3d struct for serialization. */
  public static final Pose3dStruct struct = new Pose3dStruct();
}
