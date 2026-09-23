Roles
=====

CEA App has two independent, non-overlapping permission systems: a small set of **global groups** that apply
account-wide, and a set of **department roles** that only apply within the one department they were granted in.
Almost everything a member can manage day to day (posts, events, inventory, lendings, memories, qualifications,
who else is in the department) is gated by a department role, not a global group.

Global groups
-------------

Global groups are stored on the user's account itself (not tied to any department) and are checked before
department roles are even considered.

.. list-table::
   :header-rows: 1
   :widths: 20 80

   * - Group
     - Grants
   * - ``admin``
     - Full control of everything in the app: every department role in every department, the ability to
       promote other users to ``admin``, and both of the groups below. A global admin never needs an explicit
       department role -- every department-scoped permission check treats a global admin as if they held every
       role in every department.
   * - ``users_manager``
     - Read-only visibility into the full user list (``GET /users``) regardless of department membership. Does
       **not** grant any account-mutating capability -- it cannot be used to promote itself or anyone else to
       ``admin``, since a non-admin group must never be able to grant admin.
   * - ``members_manager``
     - Manages the federation member roster (the list of the club's registered members, independent of who has
       an account in the app) and can see members' unmasked contact details.

How to grant a global group
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

- ``admin`` -- an existing admin promotes another user by calling ``POST /users/{sub}/promote``. This is
  additive only (it adds the group if missing); there is currently no route to demote an admin, and no route to
  remove any global group once granted.
- ``users_manager`` and ``members_manager`` -- there is currently no API route to grant either of these. They
  can only be added by editing the target user's ``groups`` column directly in the database.

Department roles
-----------------

Department roles are granted per member, per department: the same person can be, for example, an ``ADMIN`` of
one department and hold no role at all in another. A role only takes effect once the member's join request has
been **confirmed** -- an unconfirmed (pending) member's roles, if any are set, are not honored by any permission
check until a department ``ADMIN`` or ``PEOPLE_MANAGER`` confirms them.

There are 8 department roles:

.. list-table::
   :header-rows: 1
   :widths: 20 80

   * - Role
     - Grants
   * - ``ADMIN``
     - Full control of this department, including its settings and every other role below. Holding ``ADMIN``
       automatically satisfies every other role's permission check in that department -- it is never necessary
       (or possible) to also tick the other roles alongside it.
   * - ``PEOPLE_MANAGER``
     - Approve or deny join requests, and manage the department's members (including seeing the full member
       roster -- sub, roles and confirmation status -- which a plain member cannot). Also grants visibility into
       those members' full user records through ``GET /users``.
   * - ``INVENTORY_MANAGER``
     - Create and edit the department's inventory items and item types.
   * - ``LENDING_MANAGER``
     - Manage the department's lendings (confirming, marking items given/returned, reviewing submitted memories
       of a lending).
   * - ``MEMORY_MANAGER``
     - Manage the department's memories.
   * - ``CONTENT_MANAGER``
     - Create and edit the department's posts and events.
   * - ``QUALIFICATIONS_MANAGER``
     - Create, edit and delete the department's qualification definitions, and grant/revoke them to confirmed
       members. Implies ``EXAMINER``, so it is never necessary to also grant that role alongside it.
   * - ``EXAMINER``
     - Grant and revoke the department's existing qualifications to/from confirmed members (but not create,
       rename or delete the qualifications themselves -- that needs ``QUALIFICATIONS_MANAGER``).

A member can hold any combination of these roles at once (except that ``ADMIN`` and ``QUALIFICATIONS_MANAGER``
each already imply others, so there is no need to also select what they imply).

How admins should grant department roles
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

In the app, open the department's member list and select a confirmed member to open their role editor, where
every role can be toggled on or off and saved. Under the hood this calls::

   PATCH /departments/{departmentId}/members/{memberId}/roles

This call **replaces the member's entire set of roles** with whatever is sent -- it is not additive, so the
role editor always sends the member's full new set, not just what changed.

Only a department ``ADMIN`` (or a global admin) may call this endpoint -- deliberately stricter than every
other department-scoped action, since assigning roles (``ADMIN`` included) is privilege-escalation-capable: a
lesser role, such as ``PEOPLE_MANAGER``, must never be able to grant itself or anyone else more access than it
already has.

.. note::

   A brand-new department has no members yet, so nobody can hold a role in it at creation time. Creating a
   department is therefore always global-admin-only in practice, even though the underlying check is "holds
   ``ADMIN`` in this department" -- the same check editing an existing department uses.
