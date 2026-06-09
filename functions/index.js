const { onValueCreated } = require("firebase-functions/v2/database");
const { initializeApp } = require("firebase-admin/app");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

exports.notifyNewLostItem = onValueCreated(
  {
    ref: "/lost_items/{itemId}",
    instance: "lost-and-found-d65bc-default-rtdb",
    region: "us-central1",
  },
  async (event) => {
    const item = event.data.val();
    if (!item || !item.title) return;

    return getMessaging().send({
      notification: {
        title: `Lost: ${item.title}`,
        body: `${item.category} · ${item.locationName || "Unknown location"}`,
      },
      data: {
        itemId: event.params.itemId,
        itemType: "lost",
      },
      topic: "new_items",
    });
  }
);

exports.notifyNewFoundItem = onValueCreated(
  {
    ref: "/found_items/{itemId}",
    instance: "lost-and-found-d65bc-default-rtdb",
    region: "us-central1",
  },
  async (event) => {
    const item = event.data.val();
    if (!item || !item.title) return;

    return getMessaging().send({
      notification: {
        title: `Found: ${item.title}`,
        body: `${item.category} · ${item.locationName || "Unknown location"}`,
      },
      data: {
        itemId: event.params.itemId,
        itemType: "found",
      },
      topic: "new_items",
    });
  }
);
